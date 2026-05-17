package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rag_chat_message")
public class RagChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RagChatSession session;

    @Column(name = "message_order", nullable = false)
    private Integer messageOrder;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public enum MessageType {
        USER, AI
    }

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RagChatSession getSession() { return session; }
    public void setSession(RagChatSession session) { this.session = session; }
    public Integer getMessageOrder() { return messageOrder; }
    public void setMessageOrder(Integer messageOrder) { this.messageOrder = messageOrder; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
