package com.vela.velaCMS.controller;

import com.vela.velaCMS.dto.request.CreatePostRequest;
import com.vela.velaCMS.dto.request.PostFrontMatterRequest;
import com.vela.velaCMS.dto.request.UpdatePostContentRequest;
import com.vela.velaCMS.dto.response.PostResponse;
import com.vela.velaCMS.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cms/post")
public class PostController {

    private final PostService service;

    @Autowired
    public PostController(PostService service) {
        this.service = service;
    }

    @GetMapping("/{slug}/description")
    public ResponseEntity<?> getDescription(@PathVariable String slug, @AuthenticationPrincipal UserDetails userDetails) {
        PostResponse description = service.getDescription(userDetails.getUsername(), slug);
        return description != null ? ResponseEntity.ok(description) : ResponseEntity.notFound().build();
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return service.createPost(request, userDetails.getUsername());
    }

    @DeleteMapping("/delete/{slug}")
    public ResponseEntity<?> deletePost(@PathVariable String slug, @AuthenticationPrincipal UserDetails userDetails) {
        service.deletePost(userDetails.getUsername(), slug);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateFrontMatter/{slug}")
    public ResponseEntity<?> updatePostFrontMatter(@PathVariable String slug, @RequestBody PostFrontMatterRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return service.updatePostFrontMatter(request, userDetails.getUsername(), slug);
    }

    @PutMapping("updateContent/{slug}")
    public ResponseEntity<?> updatePostContent(@PathVariable String slug, @RequestBody UpdatePostContentRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return service.updatePostContent(request, userDetails.getUsername(), slug);
    }
}
