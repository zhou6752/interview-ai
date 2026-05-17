package com.interviewai.service;

import com.interviewai.entity.InterviewQuestion;
import com.interviewai.repository.InterviewQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class QuestionService {

    private final InterviewQuestionRepository questionRepository;

    private static final List<String> FALLBACK_ORDER = Arrays.asList("hard", "medium", "easy");

    public QuestionService(InterviewQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public InterviewQuestion getNextQuestion(String category, String difficulty, List<Long> askedQuestionIds) {
        if (askedQuestionIds == null) {
            askedQuestionIds = new ArrayList<>();
        }

        int startIndex = FALLBACK_ORDER.indexOf(difficulty);
        if (startIndex == -1) {
            startIndex = FALLBACK_ORDER.size() - 1;
        }

        for (int i = startIndex; i < FALLBACK_ORDER.size(); i++) {
            String currentDifficulty = FALLBACK_ORDER.get(i);

            long count = questionRepository.countByCategoryAndDifficulty(category, currentDifficulty);
            if (count == 0) {
                continue;
            }

            InterviewQuestion question = questionRepository.findRandomByCategoryAndDifficulty(
                    category, currentDifficulty, askedQuestionIds);
            if (question != null) {
                return question;
            }
        }

        return null;
    }
}