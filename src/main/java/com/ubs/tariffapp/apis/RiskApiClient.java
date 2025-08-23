package com.ubs.tariffapp.apis;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RiskApiClient {

    private static final String API_URL = "https://example-api.com"; // Replace with real API

    public static String fetchRiskData() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                              .uri(URI.create(API_URL))
                              .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // To-do: Add json parsing
                return response.body();
            } else {
                System.err.println("Error: HTTP " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            e.printStackTrace();
        }
        
        return null;
    }
}
