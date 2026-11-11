package com.trexwallet.posts.web;

import com.trexwallet.posts.model.Post;
import com.trexwallet.posts.repo.PostRepository;
import com.trexwallet.posts.security.TelegramAuth;
import com.trexwallet.posts.security.TelegramUser;
import com.trexwallet.posts.service.MediaClient;
import com.trexwallet.posts.service.PostSocket;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PostController {
    private final PostRepository posts; private final TelegramAuth telegram;
    private final MediaClient media; private final PostSocket socket;
    public PostController(PostRepository posts, TelegramAuth telegram, MediaClient media, PostSocket socket) {
        this.posts=posts; this.telegram=telegram; this.media=media; this.socket=socket;
    }
    @GetMapping("/health") public Map<String,String> health(){return Map.of("status","ok");}

    @GetMapping("/posts")
    public Map<String,Object> list() {
        List<Map<String,Object>> result = posts.findTop100ByOrderByCreatedAtDesc().stream().map(this::dto).toList();
        return Map.of("posts", result);
    }

    @PostMapping(value="/posts", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@RequestHeader(value="X-Telegram-Init-Data", required=false) String initData,
            @RequestParam(required=false, defaultValue="") String title,
            @RequestParam String description, @RequestParam(required=false, defaultValue="") String link,
            @RequestPart(required=false) MultipartFile mediaFile,
            @RequestParam(required=false) MultipartFile media) {
        try {
            TelegramUser user = telegram.verify(initData);
            String cleanDescription = description == null ? "" : description.trim();
            if (cleanDescription.isBlank() || cleanDescription.length() > 5000)
                return ResponseEntity.badRequest().body(Map.of("message","Description is required and must be under 5,000 characters."));
            if (title.length() > 100) return ResponseEntity.badRequest().body(Map.of("message","Title is too long."));
            if (!link.isBlank()) {
                URI uri = URI.create(link);
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null)
                    return ResponseEntity.badRequest().body(Map.of("message","Link must be a valid http or https URL."));
            }
            Post post = new Post(); post.setTitle(title.trim()); post.setDescription(cleanDescription);
            post.setLink(link.trim().isBlank()?null:link.trim());
            post.setTelegramUserId(user.id()); post.setTelegramUsername(user.username());
            post.setTelegramFirstName(user.firstName()); post.setTelegramLastName(user.lastName()); post.setTelegramPhotoUrl(user.photoUrl());
            MultipartFile chosen = mediaFile != null && !mediaFile.isEmpty() ? mediaFile : media;
            if (chosen != null && !chosen.isEmpty()) {
                if (chosen.getSize() > 50L*1024*1024) return ResponseEntity.badRequest().body(Map.of("message","Media must be 50 MB or smaller."));
                String type = chosen.getContentType() == null ? "" : chosen.getContentType();
                if (!type.startsWith("image/") && !type.startsWith("video/"))
                    return ResponseEntity.badRequest().body(Map.of("message","Only image and video files are supported."));
                MediaClient.UploadedMedia uploaded = media.upload(chosen);
                post.setMediaUrl(uploaded.url()); post.setMediaType(uploaded.mediaType());
            }
            Post saved = posts.save(post);
            Map<String,Object> response = dto(saved); socket.publish("post:created", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) { return ResponseEntity.status(401).body(Map.of("message",e.getMessage())); }
          catch (Exception e) { return ResponseEntity.status(500).body(Map.of("message",e.getMessage()==null?"Could not create post.":e.getMessage())); }
    }

    private Map<String,Object> dto(Post p) {
        Map<String,Object> user = new LinkedHashMap<>();
        user.put("id",p.getTelegramUserId()); user.put("username",p.getTelegramUsername());
        user.put("firstName",p.getTelegramFirstName()); user.put("lastName",p.getTelegramLastName()); user.put("photoUrl",p.getTelegramPhotoUrl());
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("id",p.getId()); out.put("title",p.getTitle()); out.put("description",p.getDescription());
        out.put("link",p.getLink()); out.put("mediaUrl",p.getMediaUrl()); out.put("mediaType",p.getMediaType());
        out.put("telegramUser",user); out.put("createdAt",p.getCreatedAt()); return out;
    }
}
