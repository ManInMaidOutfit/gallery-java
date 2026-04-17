package com.gallery.gallery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class ImgbbService {

    @Value("${imgbb.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadImage(MultipartFile file) throws Exception {
        // Конвертируем файл в base64
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        
        // Формируем запрос
        String url = "https://api.imgbb.com/1/upload?key=" + apiKey;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("image=" + base64Image))
                .build();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Парсим JSON ответ
        JsonNode json = objectMapper.readTree(response.body());
        
        if (json.has("data")) {
            return json.get("data").get("url").asText();
        } else {
            throw new Exception("Upload failed: " + response.body());
        }
    }
}