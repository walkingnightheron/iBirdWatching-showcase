package com.shaojiawen.ibirdwatchingserver.controller;

import com.shaojiawen.ibirdwatchingserver.common.ApiResponse;
import com.shaojiawen.ibirdwatchingserver.common.BizException;
import com.shaojiawen.ibirdwatchingserver.common.ErrorCode;
import com.shaojiawen.ibirdwatchingserver.common.RateLimiter;
import com.shaojiawen.ibirdwatchingserver.proxy.GeocodeProxyService;
import com.shaojiawen.ibirdwatchingserver.proxy.TileProxyService;
import com.shaojiawen.ibirdwatchingserver.proxy.TileTokenService;
import com.shaojiawen.ibirdwatchingserver.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final TileProxyService tileProxyService;
    private final GeocodeProxyService geocodeProxyService;
    private final TileTokenService tileTokenService;
    private final RateLimiter rateLimiter;

    public ProxyController(TileProxyService tileProxyService,
                           GeocodeProxyService geocodeProxyService,
                           TileTokenService tileTokenService,
                           RateLimiter rateLimiter) {
        this.tileProxyService = tileProxyService;
        this.geocodeProxyService = geocodeProxyService;
        this.tileTokenService = tileTokenService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/tile-token")
    public ApiResponse<Map<String, String>> tileToken() {
        checkLimit("tile-token", 10);
        Long userId = CurrentUser.id();
        return ApiResponse.ok(Map.of("token", tileTokenService.issue(userId)));
    }

    @GetMapping("/tiles/{layer}/{z}/{x}/{y}")
    public ResponseEntity<byte[]> tile(@PathVariable String layer,
                                       @PathVariable int z,
                                       @PathVariable int x,
                                       @PathVariable String y,
                                       @RequestParam(required = false) String token) {
        checkLimit("tile", 1200);
        // osmdroid 等客户端在 URL 末尾自动附加 ".png"，此处剥离兼容；
        // 鉴权支持 Authorization 头或短期瓦片 token 两种方式（token 由 /tile-token 签发）
        Long userId = CurrentUser.id();
        boolean tokenOk = tileTokenService.verify(token);
        log.info("tile req layer={} z={} x={} y={} userId={} tokenOk={}", layer, z, x, y, userId, tokenOk);
        if (userId == null && !tokenOk) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        String yValue = y.replaceAll("\\.(png|jpg|webp)$", "");
        byte[] bytes = tileProxyService.getTile(layer, z, x, Integer.parseInt(yValue));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    @GetMapping("/geocode/reverse")
    public ApiResponse<GeocodeProxyService.GeocodeResult> reverse(@RequestParam double lng,
                                                                  @RequestParam double lat) {
        checkLimit("geocode", 20);
        return ApiResponse.ok(geocodeProxyService.reverse(lng, lat));
    }

    private void checkLimit(String action, int limit) {
        Long userId = CurrentUser.id();
        if (!rateLimiter.tryAcquire(action, String.valueOf(userId), limit, Duration.ofMinutes(1))) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
    }
}
