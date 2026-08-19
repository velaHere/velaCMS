package com.vela.gramstore.service;

import com.vela.gramstore.dto.response.ImageUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
            "image/png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47},
            "image/jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46} // "RIFF"
    );

    private final Path imageFolder;

    public ImageService(@Value("${app.storage.root}") String storageRoot) {
        this.imageFolder = Paths.get(storageRoot, "images");
    }

    public ResponseEntity<Resource> getImage(String imageName) {
        try {
            String bucket = imageName.substring(0,2);
            Path imagePath = imageFolder
                    .resolve(bucket)
                    .resolve(imageName);

            if(!Files.exists(imagePath)) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(imagePath);

            String contentType =
                    Files.probeContentType(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to load image {}", imageName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<ImageUploadResponse> uploadImage(MultipartFile file) {

        try {
            if(file.isEmpty())
                return ResponseEntity.badRequest().build();

            if(file.getSize() > MAX_SIZE_BYTES) {
                return ResponseEntity.badRequest().build();
            }

            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());
            if(!ALLOWED_TYPES.contains(mimeType))
                return ResponseEntity.badRequest().build();

            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                is.read(header);
            }
            byte[] expected = MAGIC_NUMBERS.get(mimeType);
            if (expected != null && !startsWith(header, expected)) {
                throw new IllegalArgumentException("Invalid file signature for " + mimeType);
            }

            String imageName = new ObjectId() + "." + getExtension(file.getOriginalFilename());

            String shard = imageName.substring(0, 2);
            Path folder = imageFolder.resolve(shard);

            Files.createDirectories(folder);
            Path target = folder.resolve(imageName);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(
                    new ImageUploadResponse(imageName)
            );

        } catch (Exception e) {
            log.error("Failed to upload Image");
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getExtension(String filename) {

        if (filename == null) {
            return "";
        }

        int dot = filename.lastIndexOf('.');

        if (dot == -1) {
            return "";
        }

        return filename.substring(dot + 1)
                .toLowerCase();
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
