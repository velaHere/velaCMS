package com.vela.velaCMS.controller;

import com.vela.velaCMS.dto.response.PageResponse;
import com.vela.velaCMS.dto.response.PostCardResponse;
import com.vela.velaCMS.dto.response.PostResponse;
import com.vela.velaCMS.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    private final PostService postService;

    @Autowired
    public PublicController(
            PostService service
    ) {
        this.postService = service;
    }

    @GetMapping("/{username}/posts/{page}/{limit}")
    public ResponseEntity<?> getAllPosts(@PathVariable String username, @PathVariable int page, @PathVariable int limit) {
        PageResponse<PostCardResponse> response = postService.getAllPosts(username, null, page, limit).getOrThrow();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{username}/posts/{category}/{page}/{limit}")
    public ResponseEntity<?> getAllPosts(@PathVariable String username, @PathVariable String category, @PathVariable int page, @PathVariable int limit) {
        PageResponse<PostCardResponse> response = postService.getAllPosts(username, category, page, limit).getOrThrow();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{username}/post/{slug}")
    public ResponseEntity<?> getPostContent(@PathVariable String username, @PathVariable String slug) {
        PostResponse response = postService.getPost(username, slug);
        if(response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().body(response);
    }
}
