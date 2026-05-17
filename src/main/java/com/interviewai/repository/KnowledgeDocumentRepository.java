package com.interviewai.repository;

import com.interviewai.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<KnowledgeDocument> findByUserIdAndDocumentName(Long userId, String documentName);

    @Query("SELECT DISTINCT k.documentName FROM KnowledgeDocument k WHERE k.userId = :userId")
    List<String> findDistinctDocumentNamesByUserId(Long userId);

    void deleteByUserIdAndDocumentName(Long userId, String documentName);

    long countByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE KnowledgeDocument k SET k.queryCount = k.queryCount + 1 WHERE k.userId = :userId AND k.documentName = :documentName")
    void incrementQueryCount(Long userId, String documentName);
}