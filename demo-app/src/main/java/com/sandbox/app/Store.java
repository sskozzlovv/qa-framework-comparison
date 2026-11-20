package com.sandbox.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory data of the shop. Every user has an isolated cart and order list. Thread-safe. */
final class Store {

    /** An order stays PROCESSING for this long after it was placed, then becomes COMPLETED. */
    static final long PROCESSING_MS = 5_000;

    record Product(int id, String name, String category, int priceCents) { }

    record CartItem(int productId, String name, int priceCents, int qty) { }

    record Order(String id, List<CartItem> items, int totalCents, long createdAt,
                 String firstName, String lastName, String zip) {
        String status() {
            return System.currentTimeMillis() - createdAt < PROCESSING_MS ? "PROCESSING" : "COMPLETED";
        }
    }

    static final class UserData {
        final Map<Integer, CartItem> cart = new java.util.LinkedHashMap<>();
        final List<Order> orders = new ArrayList<>();
    }

    static final List<Product> PRODUCTS = List.of(
            new Product(1, "Trail Backpack", "gear", 2999),
            new Product(2, "Clip Bike Light", "gear", 999),
            new Product(3, "Cotton T-Shirt", "clothing", 1599),
            new Product(4, "Fleece Jacket", "clothing", 4999),
            new Product(5, "Wool Socks", "clothing", 799),
            new Product(6, "Rain Poncho", "clothing", 1299),
            new Product(7, "Steel Bottle", "gear", 1899),
            new Product(8, "Camp Mug", "gear", 699),
            new Product(9, "Head Lamp", "gear", 2499),
            new Product(10, "Map Case", "gear", 1099),
            new Product(11, "Compass", "gear", 1499),
            new Product(12, "Sun Hat", "clothing", 1199));

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final AtomicInteger orderSeq = new AtomicInteger(100_000);

    String createToken(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, username);
        return token;
    }

    String userByToken(String token) {
        return token == null ? null : tokens.get(token);
    }

    private UserData data(String username) {
        return users.computeIfAbsent(username, u -> new UserData());
    }

    List<Product> products(String query, String sort) {
        String q = query == null ? "" : query.trim().toLowerCase();
        Comparator<Product> order = switch (sort == null ? "" : sort) {
            case "price-asc" -> Comparator.comparingInt(Product::priceCents);
            case "price-desc" -> Comparator.comparingInt(Product::priceCents).reversed();
            case "name-desc" -> Comparator.comparing(Product::name).reversed();
            default -> Comparator.comparing(Product::name);
        };
        return PRODUCTS.stream().filter(p -> p.name().toLowerCase().contains(q)).sorted(order).toList();
    }

    Product product(int id) {
        return PRODUCTS.stream().filter(p -> p.id() == id).findFirst()
                .orElseThrow(() -> new ApiException(404, "Unknown product " + id));
    }

    Map<String, Object> cart(String username) {
        UserData d = data(username);
        synchronized (d) {
            List<CartItem> items = List.copyOf(d.cart.values());
            int total = items.stream().mapToInt(i -> i.priceCents() * i.qty()).sum();
            int count = items.stream().mapToInt(CartItem::qty).sum();
            return Map.of("items", items, "totalCents", total, "count", count);
        }
    }

    Map<String, Object> addToCart(String username, int productId) {
        Product p = product(productId);
        UserData d = data(username);
        synchronized (d) {
            d.cart.merge(productId, new CartItem(p.id(), p.name(), p.priceCents(), 1),
                    (old, one) -> new CartItem(old.productId(), old.name(), old.priceCents(), old.qty() + 1));
        }
        return cart(username);
    }

    Map<String, Object> removeFromCart(String username, int productId) {
        UserData d = data(username);
        synchronized (d) {
            d.cart.remove(productId);
        }
        return cart(username);
    }

    Order placeOrder(String username, String firstName, String lastName, String zip) {
        UserData d = data(username);
        synchronized (d) {
            if (d.cart.isEmpty()) {
                throw new ApiException(400, "Your cart is empty");
            }
            List<CartItem> items = List.copyOf(d.cart.values());
            int total = items.stream().mapToInt(i -> i.priceCents() * i.qty()).sum();
            Order order = new Order("ORD-" + orderSeq.incrementAndGet(), items, total,
                    System.currentTimeMillis(), firstName, lastName, zip);
            d.orders.add(order);
            d.cart.clear();
            return order;
        }
    }

    Map<String, Object> orders(String username, int offset, int limit) {
        UserData d = data(username);
        synchronized (d) {
            List<Order> sorted = d.orders.stream()
                    .sorted(Comparator.comparingLong(Order::createdAt).reversed())
                    .toList();
            List<Map<String, Object>> page = sorted.stream().skip(offset).limit(limit).map(Store::view).toList();
            return Map.of("items", page, "total", sorted.size());
        }
    }

    static Map<String, Object> view(Order o) {
        return Map.of("id", o.id(), "status", o.status(), "totalCents", o.totalCents(),
                "itemCount", o.items().stream().mapToInt(CartItem::qty).sum(), "createdAt", o.createdAt());
    }

    // ---------- test-only operations ----------

    Map<String, Object> cleanup(String username) {
        UserData d = users.remove(username);
        if (d == null) {
            return Map.of("removedOrders", 0, "removedCartItems", 0);
        }
        synchronized (d) {
            return Map.of("removedOrders", d.orders.size(), "removedCartItems", d.cart.size());
        }
    }

    int seedOrders(String username, int count) {
        UserData d = data(username);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        synchronized (d) {
            for (int i = 0; i < count; i++) {
                Product p = PRODUCTS.get(rnd.nextInt(PRODUCTS.size()));
                CartItem item = new CartItem(p.id(), p.name(), p.priceCents(), 1);
                // seeded orders are old -> already COMPLETED; distinct timestamps keep the order stable
                long createdAt = System.currentTimeMillis() - 86_400_000L - (count - i) * 1000L;
                d.orders.add(new Order("ORD-" + orderSeq.incrementAndGet(), List.of(item), p.priceCents(),
                        createdAt, "Seed", "User", "00000"));
            }
            return d.orders.size();
        }
    }

    Map<String, Object> state(String username) {
        UserData d = users.get(username);
        if (d == null) {
            return Map.of("cartItems", 0, "orders", 0);
        }
        synchronized (d) {
            return Map.of("cartItems", d.cart.values().stream().mapToInt(CartItem::qty).sum(), "orders", d.orders.size());
        }
    }

    void reset() {
        users.clear();
        tokens.clear();
    }
}
