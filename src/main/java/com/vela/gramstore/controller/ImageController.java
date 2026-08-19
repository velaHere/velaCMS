package com.vela.gramstore.controller;

import com.vela.gramstore.dto.response.ImageUploadResponse;
import com.vela.gramstore.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageService imageService;

    @Autowired
    public ImageController(ImageService service) {
        this.imageService = service;
    }

    @GetMapping("{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        return imageService.getImage(imageName);
    }

    @PostMapping
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return imageService.uploadImage(file);
    }
}
