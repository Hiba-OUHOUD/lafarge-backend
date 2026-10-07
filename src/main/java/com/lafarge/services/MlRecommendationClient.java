package com.lafarge.services;

import jakarta.ejb.Stateless;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class MlRecommendationClient {

    private static final String ML_SERVICE_URL = "http://localhost:5000";

    /**
     * Appelle le microservice Python et retourne la liste des IDs de formations recommandées.
     */
    public List<Long> getRecommendations(Long userId, int topN) {
        List<Long> trainingIds = new ArrayList<>();

        try {
            URL url = new URL(ML_SERVICE_URL + "/recommend/" + userId + "?top=" + topN);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.err.println(">>> ML Service error: HTTP " + conn.getResponseCode());
                return trainingIds;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Parser le JSON pour extraire les "trainingId"
            Pattern pattern = Pattern.compile("\"trainingId\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(response.toString());
            while (matcher.find()) {
                trainingIds.add(Long.parseLong(matcher.group(1)));
            }

            System.out.println(">>> ML recommandations reçues pour user " + userId + " : " + trainingIds);

        } catch (Exception e) {
            System.err.println(">>> Erreur appel ML service : " + e.getMessage());
        }

        return trainingIds;
    }
}
