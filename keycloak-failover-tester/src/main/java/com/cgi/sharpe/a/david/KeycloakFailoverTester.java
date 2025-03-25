package com.cgi.sharpe.a.david;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class KeycloakFailoverTester {
    public static void main(String[] args) {
        String authEndpoint = "https://common-logon-test.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/token";
        String clientId = "{CLIENT_ID}";
        String clientSecret = System.getenv("{CLIENT_ID}");

        int totalRequests = 10;
        int threads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        HttpClient client = HttpClient.newHttpClient();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalResponseTime = new AtomicInteger(0);
        AtomicInteger route1Count = new AtomicInteger(0);
        AtomicInteger route2Count = new AtomicInteger(0);

        long testStartTime = System.currentTimeMillis(); // Track start time

        for (int i = 0; i < totalRequests; i++) {
            int requestId = i;
            executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    String requestBody = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

                    String forcedRoute = ".1"; // Change to ".2" to target the second instance

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(authEndpoint))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Cookie", "ROUTEID=" + forcedRoute) // Manually set ROUTEID
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int responseTime = (int) (System.currentTimeMillis() - startTime);
                    totalResponseTime.addAndGet(responseTime);

                    // Extract ROUTEID from headers
                    String routeId = response.headers().firstValue("Set-Cookie")
                            .map(cookie -> extractRouteId(cookie))
                            .orElse("Unknown");

                    if (routeId.equals("1")) {
                        route1Count.incrementAndGet();
                    } else if (routeId.equals("2")) {
                        route2Count.incrementAndGet();
                    }

                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                        System.out.println("✅ Request " + requestId + " | 200 OK | Time: " + responseTime + "ms | ROUTEID: " + routeId);
                    } else {
                        failureCount.incrementAndGet();
                        System.err.println("❌ Request " + requestId + " | Error " + response.statusCode() + " | Time: " + responseTime + "ms | ROUTEID: " + routeId);
                        System.err.println("   Response: " + response.body());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("❌ Request " + requestId + " failed due to exception.");
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print Summary
        long testEndTime = System.currentTimeMillis(); // Track end time
        long totalRuntime = testEndTime - testStartTime; // Calculate total runtime

        int totalExecuted = successCount.get() + failureCount.get();
        double avgResponseTime = totalExecuted > 0 ? (double) totalResponseTime.get() / totalExecuted : 0;

        System.out.println("\n===== 📊 Test Summary =====");
        System.out.println("✅ Success Count: " + successCount.get());
        System.out.println("❌ Failure Count: " + failureCount.get());
        System.out.println("📈 Avg Response Time: " + avgResponseTime + "ms");
        System.out.println("🖥️ Requests to ROUTEID 1 (tegu): " + route1Count.get());
        System.out.println("🖥️ Requests to ROUTEID 2 (skink): " + route2Count.get());
        System.out.println("⏳ Total Runtime: " + (totalRuntime / 1000.0) + " seconds (" + totalRuntime + "ms)");
        System.out.println("===========================\n");
    }

    // Helper function to extract ROUTEID from Set-Cookie header
    private static String extractRouteId(String cookieHeader) {
        // Find ROUTEID in the cookie string
        for (String cookie : cookieHeader.split(";")) {
            cookie = cookie.trim();
            if (cookie.startsWith("ROUTEID=")) {
                return cookie.substring(8).replace(".", ""); // Extract value and remove leading dot
            }
        }
        return "Unknown";
    }

}
