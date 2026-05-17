package com.interviewai.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// SessionContext —— 面试会话上下文
// 存储【一场面试】的所有状态和数据
// 这个对象会以 JSON 格式存到 Redis 里，key = "session:uuid"
// 实现了 Serializable 接口，因为 Redis 序列化需要
public class SessionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;                      // 会话唯一标识（UUID），前端通过这个 token 标识当前面试
    private String position;                   // 面试岗位：Java后端 / 前端 / 通用
    private List<Long> askedQuestionIds;       // 已经问过的题目的 id 列表（避免重复出题）
    private String currentDifficulty;          // 当前面试难度：easy / medium / hard（会逐渐递进）
    private List<QAPair> history;              // 整场面试的历史问答记录（AI 出的题 + 用户回答 + AI 点评）
    private String status;                     // 面试状态："进行中" / "已结束"
    private Integer score;                     // 最终评分（0-100），面试结束时计算

    // resumeAnalysisResult —— 简历分析结果
    // 如果用户先上传了简历再开始面试，这个字段就存着简历分析结果
    // InterviewService 会从这个结果里取 interviewQuestions 作为面试题库
    private ResumeAnalysisResult resumeAnalysisResult;

    // askedResumeQuestionIndices —— 已经问过的"简历生成题"的下标
    // 因为简历生成的题没有数据库 id，不能用 askedQuestionIds 来记录
    // 这里用下标来 tracking：比如 index=0 表示第一道简历题已经问过了
    private List<Integer> askedResumeQuestionIndices;

    // currentQuestionText —— 当前正在问用户的面试题
    // 每次调用 conductInterview 后更新为 AI 生成的下一题
    // 用户在浏览器看到的就是这行文字，回答时传回给后端
    private String currentQuestionText;

    // mode —— 面试模式
    // practice：练习模式，无时间限制，AI 像导师引导
    // simulation：模拟模式，正常评价
    // strict：严苛模式，AI 压力追问
    private String mode = "simulation";

    // questions —— 预生成的所有题目（含追问子题），面试开始时一次性由 AI 生成填充
    private List<InterviewQuestionItem> questions;

    private Long userId;

    // currentQuestionIndex —— 当前答题到第几题（0-based），答题时逐题递增
    private int currentQuestionIndex = 0;

    // 无参构造：Jackson 反序列化时需要
    // 默认值：难度从 easy 开始，状态为"进行中"，score 初始为 0
    public SessionContext() {
        this.askedQuestionIds = new ArrayList<>();
        this.history = new ArrayList<>();
        this.currentDifficulty = "easy";
        this.status = "进行中";
        this.score = 0;
        this.askedResumeQuestionIndices = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.currentQuestionIndex = 0;
    }

    // 追加一轮问答到历史记录
    public void addToHistory(QAPair pair) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(pair);
    }

    // 标记一道题已经被问过了（避免重复出题）
    // questionId 可能为 null（AI 出的题没有数据库 id），所以要判空
    public void addAskedQuestionId(Long questionId) {
        if (askedQuestionIds == null) {
            askedQuestionIds = new ArrayList<>();
        }
        if (questionId != null) {
            askedQuestionIds.add(questionId);
        }
    }

    // 标记简历生成的一道题已被问过（用下标标记）
    public void addAskedResumeQuestionIndex(int index) {
        if (askedResumeQuestionIndices == null) {
            askedResumeQuestionIndices = new ArrayList<>();
        }
        askedResumeQuestionIndices.add(index);
    }

    // ===== Getter / Setter =====
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public List<Long> getAskedQuestionIds() { return askedQuestionIds; }
    public void setAskedQuestionIds(List<Long> askedQuestionIds) { this.askedQuestionIds = askedQuestionIds; }

    public String getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(String currentDifficulty) { this.currentDifficulty = currentDifficulty; }

    public List<QAPair> getHistory() { return history; }
    public void setHistory(List<QAPair> history) { this.history = history; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public ResumeAnalysisResult getResumeAnalysisResult() { return resumeAnalysisResult; }
    public void setResumeAnalysisResult(ResumeAnalysisResult resumeAnalysisResult) { this.resumeAnalysisResult = resumeAnalysisResult; }

    public List<Integer> getAskedResumeQuestionIndices() { return askedResumeQuestionIndices; }
    public void setAskedResumeQuestionIndices(List<Integer> askedResumeQuestionIndices) { this.askedResumeQuestionIndices = askedResumeQuestionIndices; }

    public String getCurrentQuestionText() { return currentQuestionText; }
    public void setCurrentQuestionText(String currentQuestionText) { this.currentQuestionText = currentQuestionText; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<InterviewQuestionItem> getQuestions() { return questions; }
    public void setQuestions(List<InterviewQuestionItem> questions) { this.questions = questions; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
}