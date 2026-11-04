package com.luminous.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Doc bo trac nghiem that tu bang SkinQuiz, KHONG map thanh entity.
 * Ly do: cot questions kieu JSON cua MariaDB (thuc chat LONGTEXT).
 * Neu map entity, Hibernate validate schema de bao sai kieu cot.
 * Day chi doc 1 ban ghi dang hoat dong, khong sua bang goc.
 */
@Service
public class SkinQuizDataLoader {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public SkinQuizDataLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tra ve id quiz + danh sach cau hoi; rong neu DB chua co quiz. */
    public QuizData loadActive() {
        try {
            String row = jdbc.queryForObject(
                    "SELECT CONCAT(quizID, '|', questions) FROM SkinQuiz " +
                    "WHERE isActive = TRUE ORDER BY quizID ASC LIMIT 1",
                    String.class);
            if (row == null || !row.contains("|")) {
                return new QuizData(1, List.of());
            }
            int sep = row.indexOf('|');
            int quizId = Integer.parseInt(row.substring(0, sep));
            QuizJson parsed = mapper.readValue(row.substring(sep + 1), QuizJson.class);
            return new QuizData(quizId, parsed.questions() != null ? parsed.questions() : List.of());
        } catch (Exception e) {
            // DB chua import SkinQuiz hoac JSON loi: trang quiz se bao ro,
            // khong de loi lan ra trang chu hay trang dang nhap.
            return new QuizData(1, List.of());
        }
    }

    public record QuizData(int quizId, List<QuizQuestion> questions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizJson(List<QuizQuestion> questions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizQuestion(int id, String question, List<QuizAnswer> answers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizAnswer(String id, String text, String skinType) {
    }
}
