package com.pomodoro.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomodoro.app.config.AppProperties;
import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "openai")
public class OpenAiService implements AiService {
  private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
  private static final String DEFAULT_IMAGE_QUERY = "fitness motivation training athlete gym";
  private static final List<String> SPORT_MARKERS =
      List.of(
          "спорт",
          "sport",
          "gym",
          "fitness",
          "workout",
          "muscle",
          "bodybuilding",
          "run",
          "running",
          "кардио",
          "трен",
          "бег");
  private static final List<String> SPORT_IMAGE_TERMS =
      List.of("fitness", "workout", "gym", "athlete", "training", "motivation");
  private static final List<String> STUDY_MARKERS =
      List.of(
          "учеб", "study", "exam", "school", "university", "lesson", "language", "курс", "диплом");
  private static final List<String> STUDY_IMAGE_TERMS =
      List.of("study", "books", "learning", "student", "focus", "motivation");
  private static final List<String> CODE_MARKERS =
      List.of(
          "java",
          "code",
          "coding",
          "programming",
          "developer",
          "backend",
          "frontend",
          "software",
          "програм",
          "разработ");
  private static final List<String> CODE_IMAGE_TERMS =
      List.of("programming", "developer", "software", "coding", "computer", "career");

  private final AppProperties appProperties;
  private final WebClient webClient;
  private final WebClient webImageWebClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final StorageService storageService;
  private final Duration imageTimeout;

  public OpenAiService(
      AppProperties appProperties,
      WebClient openAiWebClient,
      WebClient.Builder webClientBuilder,
      StorageService storageService) {
    this.appProperties = appProperties;
    this.webClient = openAiWebClient;
    this.storageService = storageService;
    this.imageTimeout = Duration.ofSeconds(Math.max(4, appProperties.ai().imageTimeoutSeconds()));
    ExchangeStrategies imageExchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(12 * 1024 * 1024))
            .build();
    this.webImageWebClient =
        webClientBuilder
            .clone()
            .exchangeStrategies(imageExchangeStrategies)
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .baseUrl(appProperties.ai().webImageApiUrl())
            .build();
  }

  @Override
  public AiDtos.AnalyzeResult analyzeReportImage(
      byte[] imageBytes, String userComment, AiDtos.GoalContext goalContext) {
    String base64 = Base64.getEncoder().encodeToString(imageBytes);
    String instruction =
        """
                Вы проверяете фото-отчет по задачам на сегодня.
                В goalContext.description может быть блок:
                TODAY_TASKS_START
                - задача 1
                - задача 2
                TODAY_TASKS_END
                Нужно проверить, подтверждает ли фото + комментарий выполнение задач дня.
                Если доказательств недостаточно, выбирайте NEEDS_MORE_INFO.
                Если есть явное несоответствие задачам дня, выбирайте REJECTED.
                Поле explanation пишите только на русском языке, кратко и по делу.
                При REJECTED/NEEDS_MORE_INFO обязательно укажите, какие задачи не подтверждены и что исправить.
                Верните СТРОГО JSON по схеме:
                {
                  "verdict": "APPROVED|REJECTED|NEEDS_MORE_INFO",
                  "confidence": 0.0,
                  "explanation": "short and actionable"
                }
                """;
    Map<String, Object> body =
        Map.of(
            "model", appProperties.ai().visionModel(),
            "messages",
                List.of(
                    Map.of(
                        "role",
                        "user",
                        "content",
                        List.of(
                            Map.of(
                                "type",
                                "text",
                                "text",
                                instruction
                                    + "\nGoal title: "
                                    + goalContext.title()
                                    + "\nGoal description: "
                                    + goalContext.description()
                                    + "\nUser comment: "
                                    + (userComment == null ? "" : userComment)),
                            Map.of(
                                "type",
                                "image_url",
                                "image_url",
                                Map.of("url", "data:image/jpeg;base64," + base64))))),
            "response_format", Map.of("type", "json_object"));

    String response =
        webClient
            .post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.ai().openaiApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      JsonNode root = objectMapper.readTree(response);
      String content = root.path("choices").get(0).path("message").path("content").asText();
      JsonNode parsed = objectMapper.readTree(content);
      AiVerdict verdict = AiVerdict.valueOf(parsed.path("verdict").asText("NEEDS_MORE_INFO"));
      double confidence = parsed.path("confidence").asDouble(0.5);
      String explanation = parsed.path("explanation").asText("No explanation");
      return new AiDtos.AnalyzeResult(verdict, confidence, explanation);
    } catch (Exception e) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.4,
          "Не удалось корректно распознать ответ AI. Повторите запрос и добавьте более явное фото результата.");
    }
  }

  @Override
  public String chat(List<AiDtos.ChatInputMessage> messages, AiDtos.GoalContext goalContext) {
    List<Map<String, String>> msgs =
        messages.stream().map(m -> Map.of("role", m.role(), "content", m.content())).toList();
    String response =
        webClient
            .post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.ai().openaiApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                Map.of(
                    "model", appProperties.ai().textModel(), "messages", msgs, "temperature", 0.4))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    try {
      JsonNode root = objectMapper.readTree(response);
      return root.path("choices").get(0).path("message").path("content").asText();
    } catch (Exception e) {
      return "AI response parsing failed";
    }
  }

  @Override
  public AiDtos.ImageResult generateMotivationImage(
      AiDtos.GoalContext goalContext, String styleOptions) {
    return generateInternetMotivationImage(goalContext, styleOptions);
  }

  private AiDtos.ImageResult generateInternetMotivationImage(
      AiDtos.GoalContext goalContext, String styleOptions) {
    List<String> imageTags = buildImageTags(goalContext, styleOptions);
    String prompt = "Интернет-поиск по цели: " + String.join(", ", imageTags);
    try {
      String imageUrl = searchImageUrl(imageTags);
      byte[] imageBytes = downloadImageBytes(imageUrl);
      String extension = detectExtension(imageUrl, imageBytes);
      String path = storageService.storeBytes(imageBytes, "motivation", extension);
      return new AiDtos.ImageResult(path, prompt);
    } catch (Exception e) {
      log.warn("Internet image loading failed in openai mode: {}", e.getMessage());
      try {
        String backupUrl = buildPicsumUrl(imageTags);
        byte[] backupBytes = downloadImageBytes(backupUrl);
        String backupPath = storageService.storeBytes(backupBytes, "motivation", "jpg");
        return new AiDtos.ImageResult(backupPath, prompt + " (backup feed)");
      } catch (Exception backupError) {
        log.warn(
            "Backup internet image loading failed in openai mode: {}", backupError.getMessage());
      }
      String svg =
          "<svg xmlns='http://www.w3.org/2000/svg' width='1024' height='1024'><rect width='100%' height='100%' fill='#f8f7f2'/>"
              + "<text x='70' y='210' font-size='58' fill='#1d4f47'>Internet Image Unavailable</text>"
              + "<text x='70' y='300' font-size='32' fill='#374151'>"
              + escape(goalContext.title())
              + "</text>"
              + "<text x='70' y='370' font-size='26' fill='#6b7280'>"
              + escape(String.join(", ", imageTags))
              + "</text></svg>";
      String fallbackPath =
          storageService.storeBytes(svg.getBytes(StandardCharsets.UTF_8), "motivation", "svg");
      return new AiDtos.ImageResult(fallbackPath, prompt);
    }
  }

  private String buildPicsumUrl(List<String> imageTags) {
    String seedText =
        String.join("-", imageTags) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    String seed = URLEncoder.encode(seedText, StandardCharsets.UTF_8);
    return "https://picsum.photos/seed/" + seed + "/1024/1024";
  }

  private String searchImageUrl(List<String> imageTags) throws Exception {
    List<String> queries = buildSearchQueries(imageTags);
    for (String query : queries) {
      Optional<String> found = searchImageUrlByQuery(query);
      if (found.isPresent()) {
        return found.get();
      }
    }
    throw new IllegalStateException("No images matched search query");
  }

  private Optional<String> searchImageUrlByQuery(String searchQuery) throws Exception {
    String response =
        webImageWebClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("format", "json")
                        .queryParam("generator", "search")
                        .queryParam("gsrnamespace", 6)
                        .queryParam("gsrlimit", 30)
                        .queryParam("gsrsearch", searchQuery)
                        .queryParam("prop", "imageinfo")
                        .queryParam("iiprop", "url")
                        .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .block(imageTimeout);
    if (response == null || response.isBlank()) {
      return Optional.empty();
    }
    JsonNode root = objectMapper.readTree(response);
    List<String> imageUrls = extractImageUrls(root);
    if (imageUrls.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(imageUrls.get(ThreadLocalRandom.current().nextInt(imageUrls.size())));
  }

  private List<String> buildSearchQueries(List<String> imageTags) {
    List<String> terms =
        imageTags.stream()
            .filter(tag -> tag != null && !tag.isBlank())
            .map(String::trim)
            .limit(6)
            .toList();
    String first = terms.isEmpty() ? "motivation" : terms.get(0);
    String second = terms.size() > 1 ? terms.get(1) : "success";
    String third = terms.size() > 2 ? terms.get(2) : "focus";
    return List.of(
        "filetype:bitmap|drawing " + first + " " + second,
        "filetype:bitmap|drawing " + first + " motivation",
        "filetype:bitmap|drawing " + first,
        first + " " + second + " " + third,
        first + " " + second,
        "fitness motivation",
        "success inspiration");
  }

  private List<String> extractImageUrls(JsonNode root) {
    JsonNode pages = root.path("query").path("pages");
    List<String> urls = new ArrayList<>();
    if (!pages.isObject()) {
      return urls;
    }
    Iterator<JsonNode> pageIterator = pages.elements();
    while (pageIterator.hasNext()) {
      JsonNode page = pageIterator.next();
      JsonNode imageInfo = page.path("imageinfo");
      if (!imageInfo.isArray() || imageInfo.isEmpty()) {
        continue;
      }
      String rawUrl = imageInfo.get(0).path("url").asText();
      String url = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);
      if (isSupportedImageUrl(url)) {
        urls.add(url);
      }
    }
    return urls;
  }

  private byte[] downloadImageBytes(String imageUrl) {
    byte[] bytes =
        webImageWebClient
            .get()
            .uri(imageUrl)
            .header(HttpHeaders.USER_AGENT, "PomodoroWeb/1.0")
            .retrieve()
            .bodyToMono(byte[].class)
            .block(imageTimeout);
    if (!isImageBytes(bytes)) {
      throw new IllegalStateException("Downloaded payload is not a valid image");
    }
    return bytes;
  }

  private List<String> buildImageTags(AiDtos.GoalContext goalContext, String styleOptions) {
    String combinedText =
        (goalContext.title()
                + " "
                + (goalContext.description() == null ? "" : goalContext.description()))
            .toLowerCase(Locale.ROOT);
    Set<String> tags = new LinkedHashSet<>();
    if (containsAny(combinedText, SPORT_MARKERS)) {
      tags.addAll(SPORT_IMAGE_TERMS);
    } else if (containsAny(combinedText, CODE_MARKERS)) {
      tags.addAll(CODE_IMAGE_TERMS);
    } else if (containsAny(combinedText, STUDY_MARKERS)) {
      tags.addAll(STUDY_IMAGE_TERMS);
    } else {
      for (String term : DEFAULT_IMAGE_QUERY.split(" ")) {
        tags.add(term);
      }
    }
    tags.addAll(extractEnglishTerms(goalContext.title()));
    tags.addAll(extractEnglishTerms(goalContext.description()));
    tags.addAll(extractEnglishTerms(styleOptions));
    if (tags.isEmpty()) {
      tags.addAll(List.of("motivation", "success", "focus"));
    }
    return tags.stream().limit(8).toList();
  }

  private List<String> extractEnglishTerms(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    Set<String> terms = new LinkedHashSet<>();
    String[] chunks = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
    for (String chunk : chunks) {
      if (chunk.length() >= 4 && !chunk.matches(".*\\d.*")) {
        terms.add(chunk);
      }
      if (terms.size() >= 3) {
        break;
      }
    }
    return terms.stream().toList();
  }

  private boolean containsAny(String text, List<String> keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSupportedImageUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    String lower = url.toLowerCase(Locale.ROOT);
    return lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".png")
        || lower.endsWith(".webp");
  }

  private String detectExtension(String imageUrl, byte[] bytes) {
    String lower = imageUrl == null ? "" : imageUrl.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "png";
    }
    if (lower.endsWith(".webp")) {
      return "webp";
    }
    if (bytes != null && bytes.length >= 12) {
      if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
        return "png";
      }
      if ((bytes[0] & 0xFF) == 0x52
          && bytes[1] == 0x49
          && bytes[2] == 0x46
          && bytes[3] == 0x46
          && bytes[8] == 0x57
          && bytes[9] == 0x45
          && bytes[10] == 0x42
          && bytes[11] == 0x50) {
        return "webp";
      }
    }
    return "jpg";
  }

  private boolean isImageBytes(byte[] bytes) {
    if (bytes == null || bytes.length < 16) {
      return false;
    }
    boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
    boolean png =
        (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
    boolean webp =
        (bytes[0] & 0xFF) == 0x52
            && bytes[1] == 0x49
            && bytes[2] == 0x46
            && bytes[3] == 0x46
            && bytes[8] == 0x57
            && bytes[9] == 0x45
            && bytes[10] == 0x42
            && bytes[11] == 0x50;
    return jpeg || png || webp;
  }

  private String escape(String text) {
    return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
