package com.interviewai.repository;

import com.interviewai.entity.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {

    List<InterviewRecord> findBySessionTokenOrderByQuestionIndexAsc(String sessionToken);

    @Modifying
    @Transactional
    void deleteBySessionToken(String sessionToken);

    // 用 GROUP BY 替代 DISTINCT，避免 MySQL 的 DISTINCT + ORDER BY 非 SELECT 列不兼容
    @Query("SELECT r.sessionToken FROM InterviewRecord r WHERE r.userId = :userId GROUP BY r.sessionToken ORDER BY MAX(r.startTime) DESC")
    List<String> findDistinctSessionTokensByUserId(Long userId);

    List<InterviewRecord> findByUserIdAndSessionTokenOrderByQuestionIndexAsc(Long userId, String sessionToken);
}