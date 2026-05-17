package com.interviewai.controller;

import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI 聊天", description = "与大模型进行流式对话")
public class ChatController {

    private final DynamicChatClientFactory chatClientFactory;

    public ChatController(DynamicChatClientFactory chatClientFactory) {
        this.chatClientFactory = chatClientFactory;
    }

    @Operation(summary = "流式聊天", description = "发送用户消息，以 SSE 流式返回 AI 的回复")
    @PostMapping("/stream")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        final String apiKey = CustomApiKeyFilter.getApiKey();
        final String model = CustomApiKeyFilter.getModel();
        final String baseUrl = CustomApiKeyFilter.getBaseUrl();

        new Thread(() -> {
            try {
                chatClientFactory.create(apiKey, model, baseUrl)
                        .prompt()
                        .user(request.getMessage())
                        .stream()
                        .chatResponse()
                        .subscribe(
                                response -> {
                                    String content = response.getResult().getOutput().getContent();
                                    if (content != null) {
                                        try { emitter.send(SseEmitter.event().data(content)); } catch (Exception ignored) {}
                                    }
                                },
                                error -> {
                                    try {
                                        emitter.send(SseEmitter.event().name("error").data("AI 调用失败"));
                                        emitter.complete();
                                    } catch (Exception ignored) {}
                                },
                                () -> emitter.complete()
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}