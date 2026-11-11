package com.trexwallet.posts.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_created_at", columnList = "created_at"))
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 100) private String title;
    @Column(nullable = false, length = 5000) private String description;
    @Column(length = 2048) private String link;
    @Column(name = "media_url", length = 2048) private String mediaUrl;
    @Column(name = "media_type", length = 20) private String mediaType;
    @Column(name = "telegram_user_id", nullable = false) private Long telegramUserId;
    @Column(name = "telegram_username", length = 64) private String telegramUsername;
    @Column(name = "telegram_first_name", length = 128) private String telegramFirstName;
    @Column(name = "telegram_last_name", length = 128) private String telegramLastName;
    @Column(name = "telegram_photo_url", length = 2048) private String telegramPhotoUrl;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getLink(){return link;} public void setLink(String v){link=v;}
    public String getMediaUrl(){return mediaUrl;} public void setMediaUrl(String v){mediaUrl=v;}
    public String getMediaType(){return mediaType;} public void setMediaType(String v){mediaType=v;}
    public Long getTelegramUserId(){return telegramUserId;} public void setTelegramUserId(Long v){telegramUserId=v;}
    public String getTelegramUsername(){return telegramUsername;} public void setTelegramUsername(String v){telegramUsername=v;}
    public String getTelegramFirstName(){return telegramFirstName;} public void setTelegramFirstName(String v){telegramFirstName=v;}
    public String getTelegramLastName(){return telegramLastName;} public void setTelegramLastName(String v){telegramLastName=v;}
    public String getTelegramPhotoUrl(){return telegramPhotoUrl;} public void setTelegramPhotoUrl(String v){telegramPhotoUrl=v;}
    public Instant getCreatedAt(){return createdAt;}
}
