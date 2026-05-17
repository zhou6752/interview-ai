package com.interviewai.repository;

import com.interviewai.entity.RagChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RagChatSessionRepository extends JpaRepository<RagChatSession, Long> {

    @Query("SELECT s FROM RagChatSession s WHERE s.userId = :userId ORDER BY s.updateTime DESC")
    List<RagChatSession> findByUserIdOrderByUpdateTimeDesc(Long userId);
}
