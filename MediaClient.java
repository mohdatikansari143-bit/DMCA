package com.trexwallet.posts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class MediaClient {
    @Value("${trex.media.service-url}") private String serviceUrl;
    @Value("${trex.media.api-key}") private String apiKey;
    private final RestTemplate rest = new RestTemplate();

    public UploadedMedia upload(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Media-Api-Key", apiKey);
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename(); }
            };
            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            HttpEntity<MultiValueMap<String,Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = rest.postForEntity(serviceUrl + "/internal/media", request, JsonNode.class);
            JsonNode json = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || json == null) throw new IllegalStateException("Media service upload failed.");
            return new UploadedMedia(json.path("url").asText(), json.path("mediaType").asText());
        } catch (Exception e) { throw new IllegalStateException("Could not upload media. " + e.getMessage()); }
    }
    public record UploadedMedia(String url, String mediaType) {}
}
