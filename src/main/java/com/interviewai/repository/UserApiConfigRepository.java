package com.interviewai.repository;

import com.interviewai.entity.UserApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserApiConfigRepository extends JpaRepository<UserApiConfig, Long> {

    List<UserApiConfig> findByUserId(Long userId);

    UserApiConfig findByUserIdAndIsActiveTrue(Long userId);

    UserApiConfig findByUserIdAndUseForEmbeddingTrue(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserApiConfig c SET c.isActive = false WHERE c.userId = :userId")
    void deactivateAllForUser(@Param("userId") Long userId);
}
