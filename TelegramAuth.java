package com.trexwallet.posts.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Component
public class TelegramAuth {
    @Value("${trex.telegram.bot-token}") private String botToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public TelegramUser verify(String initData) {
        try {
            if (botToken == null || botToken.isBlank())
                throw new IllegalArgumentException("Telegram bot token is not configured.");
            if (initData == null || initData.isBlank())
                throw new IllegalArgumentException("Open this app inside Telegram.");
            Map<String,String> values = new TreeMap<>();
            for (String part : initData.split("&")) {
                String[] pair = part.split("=", 2);
                values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8));
            }
            String receivedHash = values.remove("hash");
            if (receivedHash == null) throw new IllegalArgumentException("Invalid Telegram data.");
            String authDate = values.get("auth_date");
            if (authDate == null || Instant.now().getEpochSecond() - Long.parseLong(authDate) > 86400
                    || Long.parseLong(authDate) > Instant.now().getEpochSecond() + 60)
                throw new IllegalArgumentException("Telegram session expired. Reopen the Mini App.");
            StringBuilder check = new StringBuilder();
            values.forEach((k,v) -> { if (check.length() > 0) check.append('\n'); check.append(k).append('=').append(v); });
            Mac secretMac = Mac.getInstance("HmacSHA256");
            secretMac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secret = secretMac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            Mac dataMac = Mac.getInstance("HmacSHA256");
            dataMac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] calculated = dataMac.doFinal(check.toString().getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(receivedHash);
            if (!MessageDigest.isEqual(calculated, supplied))
                throw new IllegalArgumentException("Telegram identity verification failed.");
            JsonNode user = mapper.readTree(values.get("user"));
            return new TelegramUser(user.path("id").asLong(), text(user,"username"), text(user,"first_name"),
                    text(user,"last_name"), text(user,"photo_url"));
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Could not verify Telegram identity."); }
    }
    private String text(JsonNode node, String key) { return node.hasNonNull(key) ? node.get(key).asText() : null; }
}
