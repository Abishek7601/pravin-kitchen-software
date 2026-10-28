package com.example.pravin_quotation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final String UPLOAD_ROOT = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    public String storeFile(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file: Missing extension.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type: Only JPG, JPEG, PNG, and WEBP are supported.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file MIME type: " + contentType);
        }

        // Validate image magic bytes
        validateMagicBytes(file, extension);

        try {
            Path targetDir = Paths.get(UPLOAD_ROOT, subfolder != null ? subfolder : "general");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String safeName = UUID.randomUUID() + "." + extension;
            Path destination = targetDir.resolve(safeName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + (subfolder != null ? subfolder + "/" : "") + safeName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file: " + e.getMessage(), e);
        }
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = in.read(header, 0, header.length);
            if (read < 4) {
                throw new IllegalArgumentException("Invalid file header.");
            }

            if ("jpg".equals(extension) || "jpeg".equals(extension)) {
                if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                    throw new IllegalArgumentException("Corrupt or invalid JPEG image header.");
                }
            } else if ("png".equals(extension)) {
                if ((header[0] & 0xFF) != 0x89 || (header[1] & 0xFF) != 0x50
                        || (header[2] & 0xFF) != 0x4E || (header[3] & 0xFF) != 0x47) {
                    throw new IllegalArgumentException("Corrupt or invalid PNG image header.");
                }
            } else if ("webp".equals(extension)) {
                if ((header[0] & 0xFF) != 0x52 || (header[1] & 0xFF) != 0x49
                        || (header[2] & 0xFF) != 0x46 || (header[3] & 0xFF) != 0x46) {
                    throw new IllegalArgumentException("Corrupt or invalid WEBP image header.");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file header for verification: " + e.getMessage());
        }
    }

    public void deleteFile(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            String pathPart = relativeUrl.substring("/uploads/".length());
            Path target = Paths.get(UPLOAD_ROOT, pathPart);
            if (Files.exists(target)) {
                Files.delete(target);
            }
        } catch (Exception ignored) {
        }
    }
}
