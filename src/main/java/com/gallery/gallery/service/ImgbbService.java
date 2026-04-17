package com.gallery.gallery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ImgbbService {

    @Value("${imgbb.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadImage(MultipartFile file) throws Exception {
        // 1. Конвертируем файл в base64
        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        
        // 2. URL-кодируем base64 строку
        String encodedImage = URLEncoder.encode(base64Image, StandardCharsets.UTF_8.toString());
        
        // 3. Формируем POST данные
        String postData = "key=" + apiKey + "&image=" + encodedImage;
        
        // 4. Отправляем запрос
        URL url = new URL("https://api.imgbb.com/1/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        
        // 5. Читаем ответ
        int responseCode = conn.getResponseCode();
        java.io.InputStream inputStream = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        
        System.out.println("ImgBB Response Code: " + responseCode);
        System.out.println("ImgBB Response Body: " + responseBody);
        
        // 6. Парсим JSON
        JsonNode json = objectMapper.readTree(responseBody);
        
        if (json.has("data") && json.get("data").has("url")) {
            return json.get("data").get("url").asText();
        } else if (json.has("error")) {
            String errorMsg = json.get("error").get("message").asText();
            throw new Exception("ImgBB error: " + errorMsg);
        } else {
            throw new Exception("Upload failed: " + responseBody);
        }
    }
}