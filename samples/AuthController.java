package com.shaojiawen.ibirdwatchingserver.controller;

import com.shaojiawen.ibirdwatchingserver.common.ApiResponse;
import com.shaojiawen.ibirdwatchingserver.common.BizException;
import com.shaojiawen.ibirdwatchingserver.common.ErrorCode;
import com.shaojiawen.ibirdwatchingserver.common.RateLimiter;
import com.shaojiawen.ibirdwatchingserver.model.dto.LoginRequest;
import com.shaojiawen.ibirdwatchingserver.model.dto.LoginResponse;
import com.shaojiawen.ibirdwatchingserver.model.dto.RefreshResponse;
import com.shaojiawen.ibirdwatchingserver.model.dto.RegisterRequest;
import com.shaojiawen.ibirdwatchingserver.model.dto.UserVO;
import com.shaojiawen.ibirdwatchingserver.repository.ChatConversationRepository;
import com.shaojiawen.ibirdwatchingserver.repository.CommunityPostRepository;
import com.shaojiawen.ibirdwatchingserver.repository.SightRecordRepository;
import com.shaojiawen.ibirdwatchingserver.security.CurrentUser;
import com.shaojiawen.ibirdwatchingserver.service.AuthService;
import com.shaojiawen.ibirdwatchingserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RateLimiter rateLimiter;
    private final SightRecordRepository recordRepository;
    private final CommunityPostRepository postRepository;
    private final ChatConversationRepository conversationRepository;

    public AuthController(AuthService authService, UserService userService, RateLimiter rateLimiter,
                          SightRecordRepository recordRepository,
                          CommunityPostRepository postRepository,
                          ChatConversationRepository conversationRepository) {
        this.authService = authService;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.recordRepository = recordRepository;
        this.postRepository = postRepository;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/auth/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        checkLimit("register", http.getRemoteAddr(), 3, Duration.ofHours(1));
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        checkLimit("login", http.getRemoteAddr(), 5, Duration.ofMinutes(1));
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<RefreshResponse> refresh(@RequestHeader("Authorization") String authorization) {
        String token = stripBearer(authorization);
        checkLimit("refresh", token, 30, Duration.ofMinutes(5));
        return ApiResponse.ok(authService.refresh(token));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        Long userId = CurrentUser.id();
        if (userId != null) {
            authService.logout(userId);
        }
        return ApiResponse.ok();
    }

    @GetMapping("/user/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.me(CurrentUser.id()));
    }

    @PutMapping("/user/me")
    public ApiResponse<UserVO> updateMe(@RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok(userService.updateProfile(CurrentUser.id(),
                body.get("nickname"), body.get("avatarUrl")));
    }

    @GetMapping("/user/stats")
    public ApiResponse<java.util.Map<String, Object>> stats() {
        Long userId = CurrentUser.id();
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("totalRecords", recordRepository.countByUserId(userId));
        map.put("publicRecords", recordRepository.countByUserIdAndIsPublicTrue(userId));
        map.put("posts", postRepository.countByUserId(userId));
        map.put("conversations", conversationRepository.countByUserId(userId));
        return ApiResponse.ok(map);
    }

    private void checkLimit(String bucket, String key, int limit, Duration window) {
        if (!rateLimiter.tryAcquire(bucket, key, limit, window)) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
    }

    private String stripBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.REFRESH_INVALID);
        }
        return header.substring("Bearer ".length()).trim();
    }
}
