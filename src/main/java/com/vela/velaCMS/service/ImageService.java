package com.vela.velaCMS.service;

import com.vela.velaCMS.dto.response.ImageUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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

    private final SupabaseStorageService supabaseStorageService;
    private final Tika tika;

    @Autowired
    public ImageService(
            SupabaseStorageService supabaseStorageService
    ) {
        this.supabaseStorageService = supabaseStorageService;
        this.tika = new Tika();
    }

    public ResponseEntity<Resource> getImage(String imageName) {
        try {

            if (!isValidImageName(imageName)) {
                return ResponseEntity.badRequest().build();
            }

            String storageKey = getStorageKey(imageName);

            byte[] imageData = supabaseStorageService.getBytes(storageKey);

            String contentType =
                    getContentTypeFromImageName(imageName);

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType)
                    )
                    .contentLength(imageData.length)
                    .body(
                            new ByteArrayResource(imageData)
                    );
        } catch (Exception e) {
            log.error("Failed to load image: {}", imageName, e);
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<ImageUploadResponse> uploadImage(MultipartFile file) {

        try {
            if(file.isEmpty())
                return ResponseEntity.badRequest().build();

            if(file.getSize() > MAX_SIZE_BYTES) {
                return ResponseEntity.badRequest().build();
            }

            String mimeType;

            try (InputStream inputStream = file.getInputStream()) {
                mimeType = tika.detect(inputStream);
            }

            if (!ALLOWED_TYPES.contains(mimeType))
                return ResponseEntity.badRequest().build();

            byte[] header = new byte[12];
            int bytesRead;

            try (InputStream inputStream = file.getInputStream()) {
                bytesRead = inputStream.read(header);
            }

            if (bytesRead <= 0)
                return ResponseEntity.badRequest().build();

            byte[] expected =
                    MAGIC_NUMBERS.get(mimeType);

            if (expected != null && !startsWith(header, expected)) {
                return ResponseEntity.badRequest().build();
            }

            String extension = getExtensionFromMimeType(mimeType);

            String imageName = new ObjectId() + "." + extension;

            String storageKey = getStorageKey(imageName);

            try (InputStream inputStream = file.getInputStream()) {
                supabaseStorageService.put(storageKey, inputStream, file.getSize(), mimeType);
            }

            return ResponseEntity.ok(new ImageUploadResponse(imageName));
        } catch (Exception e) {
            log.error("Failed to upload Image");
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getStorageKey(String imageName) {

        String shard = imageName.substring(0, 2);

        return "images/"
                + shard
                + "/"
                + imageName;
    }

    private String getContentTypeFromImageName(
            String imageName
    ) {

        String extension =
                getExtension(imageName);

        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default ->
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
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

    private String getExtensionFromMimeType(
            String mimeType
    ) {

        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException(
                    "Unsupported image type: " + mimeType
            );
        };
    }

    private boolean isValidImageName(String imageName) {
        return imageName.matches(
                "^[a-fA-F0-9]{24}\\.(png|jpg|webp)$"
        );
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
