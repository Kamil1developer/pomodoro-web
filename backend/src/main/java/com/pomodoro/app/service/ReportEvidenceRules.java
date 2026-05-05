package com.pomodoro.app.service;

import com.pomodoro.app.dto.AiDtos;
import com.pomodoro.app.enums.AiVerdict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class ReportEvidenceRules {
  static final String TASK_BLOCK_START = "TODAY_TASKS_START";
  static final String TASK_BLOCK_END = "TODAY_TASKS_END";
  static final String ACCEPTANCE_BLOCK_START = "ACCEPTANCE_CRITERIA_START";
  static final String ACCEPTANCE_BLOCK_END = "ACCEPTANCE_CRITERIA_END";

  private static final Set<String> COMPLETION_MARKERS =
      Set.of(
          "сделал",
          "выполнил",
          "готово",
          "завершил",
          "закончил",
          "закрыл",
          "посмотрел",
          "смотрел",
          "прочитал",
          "сдал",
          "написал",
          "решил",
          "запустил",
          "собрал",
          "протестировал",
          "completed",
          "done");
  private static final Set<String> VIDEO_MARKERS =
      Set.of(
          "youtube",
          "ютуб",
          "видео",
          "video",
          "лекци",
          "лекция",
          "урок",
          "смотрел",
          "посмотрел",
          "watch",
          "watched");
  private static final Set<String> PRACTICE_MARKERS =
      Set.of(
          "код",
          "code",
          "ide",
          "проект",
          "project",
          "конспект",
          "заметки",
          "ноутс",
          "упражнен",
          "задан",
          "решен",
          "решил",
          "документ",
          "тест",
          "tests",
          "скрипт",
          "реализ",
          "написал",
          "собрал",
          "debug",
          "рефактор",
          "зафиксировал");
  private static final Set<String> ARTIFACT_TASK_MARKERS =
      Set.of(
          "код",
          "code",
          "project",
          "проект",
          "конспект",
          "заметки",
          "документ",
          "упражнен",
          "задан",
          "тест",
          "tests",
          "лаборатор",
          "реализ",
          "скрипт");
  private static final Set<String> LECTURE_TASK_MARKERS =
      Set.of("лекци", "видео", "youtube", "ютуб", "урок", "посмотреть", "просмотр");

  private ReportEvidenceRules() {}

  static AiDtos.AnalyzeResult evaluate(String userComment, AiDtos.GoalContext goalContext) {
    List<String> todayTasks = extractTodayTasks(goalContext.description());
    if (todayTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.42,
          "Не найдены задачи на сегодня. Добавьте задачи дня, чтобы AI мог проверить доказательство выполнения.");
    }

    String normalizedComment = normalize(userComment);
    if (normalizedComment.isBlank()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.41,
          "Комментарий пустой. Укажите, какую задачу вы выполнили и какой именно результат получили.");
    }

    boolean mentionsVideo = containsAny(normalizedComment, VIDEO_MARKERS);
    boolean anyArtifactTaskToday =
        todayTasks.stream().anyMatch(ReportEvidenceRules::requiresArtifactEvidence);
    List<String> matchedTasks = matchTasks(todayTasks, normalizedComment);
    if (matchedTasks.isEmpty()) {
      if (mentionsVideo && anyArtifactTaskToday) {
        return new AiDtos.AnalyzeResult(
            AiVerdict.NEEDS_MORE_INFO,
            0.69,
            "Комментарий указывает только на просмотр видео. Для практической задачи этого недостаточно: добавьте код, конспект, документ, выполненное задание или тесты.");
      }
      return new AiDtos.AnalyzeResult(
          AiVerdict.REJECTED,
          0.78,
          "Комментарий не связан с задачами на сегодня. Ожидалось подтверждение по задачам: "
              + previewTasks(todayTasks)
              + ".");
    }

    List<String> missingTasks =
        todayTasks.stream().filter(task -> !matchedTasks.contains(task)).toList();
    boolean hasCompletionMarker = containsAny(normalizedComment, COMPLETION_MARKERS);
    boolean mentionsPracticeEvidence = containsAny(normalizedComment, PRACTICE_MARKERS);
    boolean artifactTaskMatched =
        matchedTasks.stream().anyMatch(ReportEvidenceRules::requiresArtifactEvidence);
    boolean lectureOnlyMatched = matchedTasks.stream().allMatch(ReportEvidenceRules::isLectureTask);

    if (!hasCompletionMarker && !mentionsPracticeEvidence) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.55,
          "Есть упоминание задач ("
              + previewTasks(matchedTasks)
              + "), но нет явного признака выполненного результата. Покажите итог: код, конспект, документ, тесты или другое подтверждение.");
    }

    if (artifactTaskMatched && mentionsVideo && !mentionsPracticeEvidence) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.7,
          "Сейчас подтверждён только просмотр видео. Для практической задачи этого недостаточно: добавьте доказательство результата, например код, конспект, выполненное задание или тесты.");
    }

    if (lectureOnlyMatched && mentionsVideo && hasCompletionMarker && missingTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.APPROVED,
          0.72,
          "Комментарий указывает на просмотр учебного видео по задаче дня. Для задачи формата лекции или видео этого достаточно.");
    }

    if (artifactTaskMatched
        && mentionsPracticeEvidence
        && hasCompletionMarker
        && missingTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.APPROVED,
          0.86,
          "В комментарии есть подтверждение результата по практической задаче: "
              + previewTasks(matchedTasks)
              + ". Это соответствует ожидаемому доказательству выполнения.");
    }

    if (!missingTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.63,
          "Подтверждены не все задачи дня. Уже подтверждено: "
              + previewTasks(matchedTasks)
              + ". Не хватает подтверждения по: "
              + previewTasks(missingTasks)
              + ".");
    }

    if (artifactTaskMatched && !mentionsPracticeEvidence) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.NEEDS_MORE_INFO,
          0.67,
          "Для практической задачи нужен видимый результат: код, конспект, документ, выполненное упражнение или тесты. Текущего описания недостаточно.");
    }

    if (hasCompletionMarker && missingTasks.isEmpty()) {
      return new AiDtos.AnalyzeResult(
          AiVerdict.APPROVED,
          0.74,
          "Комментарий подтверждает выполнение задачи дня: "
              + previewTasks(matchedTasks)
              + ". Для этой задачи не требуется отдельный артефакт вроде кода или конспекта.");
    }

    return new AiDtos.AnalyzeResult(
        AiVerdict.NEEDS_MORE_INFO,
        0.58,
        "Есть частичное подтверждение по задачам дня, но доказательство результата недостаточно явное. Добавьте фото результата и конкретизируйте итог в комментарии.");
  }

  static List<String> extractTodayTasks(String description) {
    if (description == null || description.isBlank()) {
      return List.of();
    }
    int start = description.indexOf(TASK_BLOCK_START);
    int end = description.indexOf(TASK_BLOCK_END);
    if (start < 0 || end <= start) {
      return List.of();
    }
    String block = description.substring(start + TASK_BLOCK_START.length(), end);
    return block
        .lines()
        .map(String::trim)
        .filter(line -> line.startsWith("- "))
        .map(line -> line.substring(2).replaceAll("\\s*\\(статус:.*\\)$", "").trim())
        .filter(line -> !line.isBlank())
        .toList();
  }

  static boolean isLectureTask(String task) {
    return containsAny(normalize(task), LECTURE_TASK_MARKERS);
  }

  static boolean requiresArtifactEvidence(String task) {
    return containsAny(normalize(task), ARTIFACT_TASK_MARKERS);
  }

  private static List<String> matchTasks(List<String> tasks, String normalizedComment) {
    List<String> matched = new ArrayList<>();
    for (String task : tasks) {
      List<String> keywords = extractKeywords(task);
      boolean anyPresent = keywords.stream().anyMatch(normalizedComment::contains);
      if (anyPresent) {
        matched.add(task);
      }
    }
    return matched;
  }

  private static List<String> extractKeywords(String source) {
    String normalized = normalize(source);
    if (normalized.isBlank()) {
      return List.of();
    }
    Set<String> tokens =
        Arrays.stream(normalized.split(" "))
            .filter(token -> token.length() >= 4)
            .limit(4)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return List.copyOf(tokens);
  }

  private static String normalize(String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static boolean containsAny(String text, Set<String> markers) {
    return markers.stream().anyMatch(text::contains);
  }

  private static String previewTasks(List<String> tasks) {
    return tasks.stream().limit(3).collect(Collectors.joining(", "));
  }
}
