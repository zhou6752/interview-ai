package com.interviewai.controller;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 聊天请求体对象
 * 用于接收前端发送的用户消息
 */
public class ChatRequest {

    /**
     * 用户输入的消息内容
     */
    @Schema(description = "用户发送的消息内容", example = "用Java写一个Hello World")
    private String message;

    /**
     * 无参构造方法，框架反序列化时需要
     */
    public ChatRequest() {
    }

    /**
     * 带参构造方法，方便手动创建对象
     *
     * @param message 用户消息
     */
    public ChatRequest(String message) {
        this.message = message;
    }

    /**
     * 获取用户消息
     *
     * @return 消息内容字符串
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息
     *
     * @param message 要设置的消息内容
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
