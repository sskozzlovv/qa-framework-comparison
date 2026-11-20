package com.compare.playwright.suites;

import com.compare.playwright.core.Config;
import com.sandbox.app.DemoApp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Shared &#64;BeforeSuite / &#64;AfterSuite logic. No browser exists yet here -> plain Java HttpClient. */
final class SuiteHooks {

    private SuiteHooks() {
    }

    static void resetBackend(String suite) {
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(Config.baseUrl() + "/api/test/reset"))
                            .header("X-Test-Key", DemoApp.TEST_KEY)
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("[" + suite + "] @BeforeSuite: backend reset -> " + response.statusCode());
        } catch (IOException e) {
            throw new IllegalStateException("Backend is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
