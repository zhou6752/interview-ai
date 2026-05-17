package com.interviewai.repository;

import com.interviewai.entity.InterviewQuestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// InterviewQuestionRepository —— 面试题库的数据访问层
// 继承 JpaRepository 后，Spring 会自动帮我们生成实现类（不需要自己写实现代码）
// JpaRepository<InterviewQuestion, Long> 的意思是：
//   第一个参数 InterviewQuestion = 要操作哪张表（对应 interview_question 表）
//   第二个参数 Long             = 那这张表的主键 id 是什么类型
// 有了它，我们就能对 interview_question 表做增删改查了
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByCategory(String category);

    List<InterviewQuestion> findByCategory(String category, Pageable pageable);

    @Query(value = "SELECT * FROM interview_question " +
           "WHERE category = :category AND difficulty = :difficulty " +
           "AND (:askedIds IS NULL OR id NOT IN (:askedIds)) " +
           "ORDER BY RAND() LIMIT 1", nativeQuery = true)
    InterviewQuestion findRandomByCategoryAndDifficulty(
            @Param("category") String category,
            @Param("difficulty") String difficulty,
            @Param("askedIds") List<Long> askedIds);

    long countByCategoryAndDifficulty(String category, String difficulty);

    // 一次查多个分类（position 映射后的 categories），按难度随机抽一题
    @Query(value = "SELECT * FROM interview_question " +
           "WHERE category IN (:categories) AND difficulty = :difficulty " +
           "AND (:askedIds IS NULL OR id NOT IN (:askedIds)) " +
           "ORDER BY RAND() LIMIT 1", nativeQuery = true)
    InterviewQuestion findRandomByCategoriesAndDifficulty(
            @Param("categories") List<String> categories,
            @Param("difficulty") String difficulty,
            @Param("askedIds") List<Long> askedIds);

    long countByCategoryInAndDifficulty(List<String> categories, String difficulty);
}
