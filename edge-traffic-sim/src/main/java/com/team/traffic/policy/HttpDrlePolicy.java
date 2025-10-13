package com.team.traffic.policy;

import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class HttpDrlePolicy {

  private final String ciServerUrl;

  public HttpDrlePolicy(String ciServerUrl) {
    this.ciServerUrl = ciServerUrl;
  }

  public String sendToCI(String observationJson) {
    try {
      // Create a URL object with the CI server endpoint
      URL url = new URL(ciServerUrl + "/drle/act");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");

      // Send the observation JSON data
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = observationJson.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      // Read the response from the server
      try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
          response.append(responseLine.trim());
        }
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("action");
      }

    } catch (Exception e) {
      e.printStackTrace();
      return "HOLD";  // Fallback action if there's an error
    }
  }
}
