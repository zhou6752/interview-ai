package com.interviewai.repository;

import com.interviewai.entity.RagChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RagChatMessageRepository extends JpaRepository<RagChatMessage, Long> {

    List<RagChatMessage> findBySessionIdOrderByMessageOrderAsc(Long sessionId);

    @Query("SELECT m FROM RagChatMessage m WHERE m.session.id = :sessionId ORDER BY m.messageOrder DESC")
    List<RagChatMessage> findRecentBySessionId(Long sessionId, Pageable pageable);
}
