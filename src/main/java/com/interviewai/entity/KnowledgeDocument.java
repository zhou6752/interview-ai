package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "file_name", length = 300)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "vector_status", length = 20)
    private String vectorStatus;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "query_count")
    private Integer queryCount;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (queryCount == null) queryCount = 0;
        if (chunkCount == null) chunkCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVectorStatus() { return vectorStatus; }
    public void setVectorStatus(String vectorStatus) { this.vectorStatus = vectorStatus; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getQueryCount() { return queryCount; }
    public void setQueryCount(Integer queryCount) { this.queryCount = queryCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}