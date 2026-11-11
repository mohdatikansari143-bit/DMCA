package com.trexwallet.posts.repo;

import com.trexwallet.posts.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findTop100ByOrderByCreatedAtDesc();
}
