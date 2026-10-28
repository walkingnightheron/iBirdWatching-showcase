package com.shaojiawen.ibirdwatchingserver.controller;

import com.shaojiawen.ibirdwatchingserver.common.ApiResponse;
import com.shaojiawen.ibirdwatchingserver.common.BizException;
import com.shaojiawen.ibirdwatchingserver.common.ErrorCode;
import com.shaojiawen.ibirdwatchingserver.common.RateLimiter;
import com.shaojiawen.ibirdwatchingserver.model.ChatConversation;
import com.shaojiawen.ibirdwatchingserver.model.ChatMessage;
import com.shaojiawen.ibirdwatchingserver.rag.ChatService;
import com.shaojiawen.ibirdwatchingserver.rag.DeepSeekClient;
import com.shaojiawen.ibirdwatchingserver.rag.KnowledgeBaseService;
import com.shaojiawen.ibirdwatchingserver.security.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    public ChatController(ChatService chatService, RateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/conversations")
    public ApiResponse<ChatConversation> create(@RequestBody(required = false) Map<String, String> body) {
        String title = body == null ? null : body.get("title");
        return ApiResponse.ok(chatService.create(CurrentUser.id(), title));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversation>> list() {
        return ApiResponse.ok(chatService.list(CurrentUser.id()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<ChatMessage>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.messages(CurrentUser.id(), id));
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        chatService.delete(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping(value = "/conversations/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = CurrentUser.id();
        if (!rateLimiter.tryAcquire("chat", String.valueOf(userId), 20, Duration.ofMinutes(1))) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        String question = body == null ? "" : body.getOrDefault("content", "").trim();
        if (question.isEmpty() || question.length() > 500) {
            throw new BizException(ErrorCode.PARAM_ERROR, "问题长度需在 1~500 字");
        }

        SseEmitter emitter = new SseEmitter(60_000L);
        new Thread(() -> {
            try {
                ChatService.AskResult result = chatService.ask(userId, id, question, delta -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data("{\"delta\":\"" + escape(delta) + "\"}"));
                    } catch (IOException ignored) {
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(
                        "{\"full\":\"" + escape(result.fullAnswer()) + "\",\"sources\":" + sourcesJson(result.hits()) + "}"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"code\":\"1402\",\"message\":\"" + escape(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        }, "chat-ask").start();
        return emitter;
    }

    private String sourcesJson(List<KnowledgeBaseService.Hit> hits) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeBaseService.Hit h = hits.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"title\":\"").append(escape(h.title()))
                    .append("\",\"source\":\"").append(escape(h.source()))
                    .append("\",\"score\":").append(String.format("%.3f", h.score())).append("}");
        }
        return sb.append("]").toString();
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
