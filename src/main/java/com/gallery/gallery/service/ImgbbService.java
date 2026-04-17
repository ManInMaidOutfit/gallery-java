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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ImgbbService {

    @Value("${imgbb.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadImage(MultipartFile file) throws Exception {
        // Конвертируем файл в base64
        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        
        // URL encode the base64 string
        String encodedImage = URLEncoder.encode(base64Image, StandardCharsets.UTF_8.toString());
        
        // Формируем запрос
        String url = "https://api.imgbb.com/1/upload?key=" + apiKey;
        
        // Вариант 1: Отправляем как form data
        String body = "image=" + encodedImage;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("ImgBB Response: " + response.body());
        
        // Парсим JSON ответ
        JsonNode json = objectMapper.readTree(response.body());
        
        if (json.has("data") && json.get("data").has("url")) {
            return json.get("data").get("url").asText();
        } else if (json.has("error")) {
            String errorMsg = json.get("error").get("message").asText();
            throw new Exception("ImgBB error: " + errorMsg);
        } else {
            throw new Exception("Upload failed: " + response.body());
        }
    }
}