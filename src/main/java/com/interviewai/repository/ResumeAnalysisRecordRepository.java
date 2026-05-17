package com.interviewai.repository;

import com.interviewai.entity.ResumeAnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeAnalysisRecordRepository extends JpaRepository<ResumeAnalysisRecord, Long> {

    List<ResumeAnalysisRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}