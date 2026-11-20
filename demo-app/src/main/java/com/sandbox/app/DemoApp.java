package com.sandbox.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sandbox Shop - the system under test for all three frameworks.
 *
 * <p>It is deliberately "unfriendly" for UI automation: every API call has random latency, a full-screen spinner
 * overlay blocks clicks while loading, the product list is re-rendered after a debounced search, a promo modal pops
 * up at a random moment, order status changes over time, dialogs open with a delay, and so on.
 *
 * <p>Users: any username [a-zA-Z0-9_.-]{3,40} with password {@code secret} is accepted (so every test can use its
 * own user = data isolation). {@code locked_*} users are locked out, {@code slow_*} users get 4x latency.
 *
 * <p>Test-only API (header {@code X-Test-Key: let-me-in}): POST /api/test/cleanup, POST /api/test/seed-orders,
 * GET /api/test/state, POST /api/test/reset.
 */
public final class DemoApp {

    public static final String TEST_KEY = "let-me-in";
    public static final String PASSWORD = "secret";

    private static DemoApp shared;

    static {
        // JDK 25 on Windows: NIO selectors use an AF_UNIX socket in the temp dir, and fail with
        // "Unable to establish loopback connection" when that path contains an 8.3 short name (C:\Users\JOHNDO~1).
        // Only effective if no socket was opened in this JVM yet - Maven also sets it via surefire argLine.
        if (System.getProperty("jdk.net.unixdomain.tmpdir") == null
                && System.getProperty("java.io.tmpdir", "").contains("~")) {
            try {
                java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("user.home"), ".sandbox-shop-tmp");
                java.nio.file.Files.createDirectories(dir);
                System.setProperty("jdk.net.unixdomain.tmpdir", dir.toString());
            } catch (IOException ignored) {
                // keep the JDK default
            }
        }
    }

    private final HttpServer server;
    private final Store store = new Store();
    private final Gson gson = new Gson();
    private final double latencyFactor = Double.parseDouble(System.getProperty("demo.latency", "1.0"));

    private DemoApp(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bindAddress(), port), 0);
        server.createContext("/", this::handle);
        // virtual threads: Thread.sleep() for simulated latency costs nothing
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    /**
     * Loopback by default: the app is not reachable from other machines. In Docker the browser of Selenium Grid
     * runs in ANOTHER container, so the app container starts with -Ddemo.host=0.0.0.0 (all interfaces).
     */
    private static InetAddress bindAddress() throws IOException {
        String host = System.getProperty("demo.host", "").trim();
        return host.isEmpty() ? InetAddress.getLoopbackAddress() : InetAddress.getByName(host);
    }

    /** Starts a new server; port 0 = any free port. */
    public static DemoApp start(int port) {
        try {
            return new DemoApp(port);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start demo app on port " + port, e);
        }
    }

    /**
     * Returns the base URL of the app under test: -DbaseUrl=... if set (app already running somewhere), otherwise
     * one in-process server shared by every test of the JVM (started on first call, stopped on JVM exit).
     */
    public static synchronized String startShared() {
        String external = System.getProperty("baseUrl", "").trim();
        if (!external.isEmpty()) {
            return external.endsWith("/") ? external.substring(0, external.length() - 1) : external;
        }
        if (shared == null) {
            shared = start(0);
            Runtime.getRuntime().addShutdownHook(new Thread(shared::stop));
            System.out.println("[demo-app] started at " + shared.baseUrl());
        }
        return shared.baseUrl();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        System.out.println("Sandbox Shop running at " + start(port).baseUrl() + "  (Ctrl+C to stop)");
    }

    // ------------------------------------------------------------------------------------------------------------

    private void handle(HttpExchange ex) {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                try {
                    handleApi(ex, ex.getRequestMethod(), path);
                } catch (ApiException e) {
                    sendJson(ex, e.status, Map.of("error", e.getMessage()));
                }
            } else {
                serveStatic(ex, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleApi(HttpExchange ex, String method, String path) throws IOException {
        Map<String, String> query = query(ex);

        if (method.equals("GET") && path.equals("/api/health")) {
            sendJson(ex, 200, Map.of("status", "UP"));
            return;
        }
        if (method.equals("GET") && path.equals("/api/report.csv")) {
            StringBuilder csv = new StringBuilder("id,name,category,price\n");
            Store.PRODUCTS.forEach(p -> csv.append(p.id()).append(',').append(p.name()).append(',')
                    .append(p.category()).append(',').append(p.priceCents() / 100.0).append('\n'));
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"products-report.csv\"");
            send(ex, 200, "text/csv; charset=utf-8", csv.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (path.startsWith("/api/test/")) {
            handleTestApi(ex, method, path, query);
            return;
        }
        if (method.equals("POST") && path.equals("/api/login")) {
            JsonObject body = body(ex);
            String username = str(body, "username");
            String password = str(body, "password");
            latency(username);
            if (username.isEmpty() || password.isEmpty()) {
                throw new ApiException(400, "Username and password are required");
            }
            if (!username.matches("[a-zA-Z0-9_.-]{3,40}") || !PASSWORD.equals(password)) {
                throw new ApiException(401, "Username and password do not match any user");
            }
            if (username.startsWith("locked_")) {
                throw new ApiException(403, "Sorry, this user has been locked out");
            }
            sendJson(ex, 200, Map.of("token", store.createToken(username), "username", username));
            return;
        }

        // ---- everything below requires a token ----
        String user = store.userByToken(ex.getRequestHeaders().getFirst("X-Auth-Token"));
        if (user == null) {
            throw new ApiException(401, "Unauthorized");
        }
        latency(user);

        if (method.equals("GET") && path.equals("/api/products")) {
            sendJson(ex, 200, store.products(query.get("q"), query.get("sort")));
        } else if (method.equals("GET") && path.equals("/api/cart")) {
            sendJson(ex, 200, store.cart(user));
        } else if (method.equals("POST") && path.equals("/api/cart")) {
            sendJson(ex, 200, store.addToCart(user, body(ex).get("productId").getAsInt()));
        } else if (method.equals("DELETE") && path.startsWith("/api/cart/")) {
            sendJson(ex, 200, store.removeFromCart(user, Integer.parseInt(path.substring("/api/cart/".length()))));
        } else if (method.equals("POST") && path.equals("/api/orders")) {
            JsonObject body = body(ex);
            Store.Order order = store.placeOrder(user, str(body, "firstName"), str(body, "lastName"), str(body, "zip"));
            sendJson(ex, 201, Store.view(order));
        } else if (method.equals("GET") && path.equals("/api/orders")) {
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            int limit = Integer.parseInt(query.getOrDefault("limit", "5"));
            sendJson(ex, 200, store.orders(user, offset, limit));
        } else {
            throw new ApiException(404, "No such endpoint: " + method + " " + path);
        }
    }

    private void handleTestApi(HttpExchange ex, String method, String path, Map<String, String> query) throws IOException {
        if (!TEST_KEY.equals(ex.getRequestHeaders().getFirst("X-Test-Key"))) {
            throw new ApiException(403, "Test API requires the X-Test-Key header");
        }
        switch (method + " " + path) {
            case "POST /api/test/cleanup" -> sendJson(ex, 200, store.cleanup(str(body(ex), "username")));
            case "POST /api/test/seed-orders" -> {
                JsonObject body = body(ex);
                int total = store.seedOrders(str(body, "username"), body.get("count").getAsInt());
                sendJson(ex, 200, Map.of("orders", total));
            }
            case "GET /api/test/state" -> sendJson(ex, 200, store.state(query.getOrDefault("username", "")));
            case "POST /api/test/reset" -> {
                store.reset();
                sendJson(ex, 200, Map.of("reset", true));
            }
            default -> throw new ApiException(404, "No such test endpoint: " + method + " " + path);
        }
    }

    /** Random 150..500 ms per call; x4 for slow_* users; scaled by -Ddemo.latency (0 = no latency). */
    private void latency(String username) {
        double factor = latencyFactor * (username != null && username.startsWith("slow_") ? 4 : 1);
        long ms = (long) (ThreadLocalRandom.current().nextInt(150, 500) * factor);
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (path.contains("..")) {
            send(ex, 400, "text/plain", "Bad path".getBytes(StandardCharsets.UTF_8));
            return;
        }
        try (InputStream in = DemoApp.class.getResourceAsStream("/static" + path)) {
            if (in == null) {
                send(ex, 404, "text/plain", ("Not found: " + path).getBytes(StandardCharsets.UTF_8));
                return;
            }
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            send(ex, 200, contentType(path), in.readAllBytes());
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private JsonObject body(HttpExchange ex) throws IOException {
        String text = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = text.isBlank() ? null : gson.fromJson(text, JsonObject.class);
        if (json == null) {
            throw new ApiException(400, "JSON body expected");
        }
        return json;
    }

    private static String str(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString().trim() : "";
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> result = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    result.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        send(ex, status, "application/json; charset=utf-8", gson.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
