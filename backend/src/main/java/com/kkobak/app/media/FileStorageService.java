package com.kkobak.app.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private final StorageProperties properties;
    private final RestClient restClient = RestClient.create();

    public FileStorageService(StorageProperties properties) { this.properties = properties; }

    public String store(MultipartFile file) {
        if (file.isEmpty() || !ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("JPG, PNG, WebP 이미지만 업로드할 수 있습니다.");
        String extension = extension(file.getOriginalFilename(), file.getContentType());
        String objectName = "cycles/%s%s".formatted(UUID.randomUUID(), extension);
        return "supabase".equalsIgnoreCase(properties.mode()) ? storeSupabase(file, objectName) : storeLocal(file, objectName);
    }

    private String storeLocal(MultipartFile file, String objectName) {
        try {
            Path target = Path.of(properties.localRoot()).resolve(objectName).normalize().toAbsolutePath();
            Path root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("올바르지 않은 파일 경로입니다.");
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "%s/%s".formatted(properties.localPublicBaseUrl().replaceAll("/$", ""), objectName);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", exception);
        }
    }

    private String storeSupabase(MultipartFile file, String objectName) {
        if (blank(properties.supabaseUrl()) || blank(properties.supabaseServiceRoleKey())) throw new IllegalStateException("Supabase Storage 설정이 필요합니다.");
        try {
            String base = properties.supabaseUrl().replaceAll("/$", "");
            restClient.put().uri("%s/storage/v1/object/%s/%s".formatted(base, properties.supabaseBucket(), objectName))
                    .header("Authorization", "Bearer " + properties.supabaseServiceRoleKey())
                    .header("apikey", properties.supabaseServiceRoleKey())
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(file.getBytes()).retrieve().toBodilessEntity();
            return "%s/storage/v1/object/public/%s/%s".formatted(base, properties.supabaseBucket(), objectName);
        } catch (IOException exception) {
            throw new IllegalStateException("이미지를 업로드하지 못했습니다.", exception);
        }
    }

    private String extension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String value = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (Set.of(".jpg", ".jpeg", ".png", ".webp").contains(value)) return value;
        }
        return switch (contentType) { case "image/png" -> ".png"; case "image/webp" -> ".webp"; default -> ".jpg"; };
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
