package com.pomodoro.app.service;

import com.pomodoro.app.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
  private final Path root;

  public StorageService(AppProperties appProperties) throws IOException {
    this.root = Path.of(appProperties.uploadsDir()).toAbsolutePath().normalize();
    Files.createDirectories(root);
    Files.createDirectories(root.resolve("avatars"));
    Files.createDirectories(root.resolve("reports"));
    Files.createDirectories(root.resolve("motivation"));
  }

  public String storeAvatar(MultipartFile file) {
    return store(file, "avatars");
  }

  public String storeReport(MultipartFile file) {
    return store(file, "reports");
  }

  public String storeMotivationBase64(String base64, String extension) {
    try {
      byte[] bytes = Base64.getDecoder().decode(base64);
      String filename = UUID.randomUUID() + "." + extension;
      Path path = root.resolve("motivation").resolve(filename);
      Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
      return publicPath("motivation", filename);
    } catch (IOException e) {
      throw new RuntimeException("Cannot store image", e);
    }
  }

  public String storeBytes(byte[] bytes, String dir, String extension) {
    try {
      String filename = UUID.randomUUID() + "." + extension;
      Path path = root.resolve(dir).resolve(filename);
      Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
      return publicPath(dir, filename);
    } catch (IOException e) {
      throw new RuntimeException("Cannot store image", e);
    }
  }

  private String store(MultipartFile file, String dir) {
    try {
      String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
      ext = (ext == null || ext.isBlank()) ? "bin" : ext;
      String filename = UUID.randomUUID() + "." + ext;
      Path path = root.resolve(dir).resolve(filename);
      Files.copy(file.getInputStream(), path);
      return publicPath(dir, filename);
    } catch (IOException e) {
      throw new RuntimeException("Cannot store file", e);
    }
  }

  private String publicPath(String dir, String filename) {
    return "/uploads/" + dir + "/" + filename;
  }

  public void deletePublicPath(String publicPath) {
    if (publicPath == null || !publicPath.startsWith("/uploads/")) {
      return;
    }
    String relative = publicPath.substring("/uploads/".length());
    Path path = root.resolve(relative).normalize();
    if (!path.startsWith(root)) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
    }
  }
}
