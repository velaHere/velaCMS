package com.vela.gramstore.controller;

import com.vela.gramstore.dto.response.PostResponse;
import com.vela.gramstore.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/{username}/post")
    public ResponseEntity<?> getAllPosts(@PathVariable String username) {
        return ResponseEntity.ok().body(postService.getAllPosts(username));
    }

    @GetMapping("/{username}/post/{slug}")
    public ResponseEntity<?> getPost(@PathVariable String username, @PathVariable String slug) {
        PostResponse response = postService.getPost(username, slug);
        if(response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().body(response);
    }
}
