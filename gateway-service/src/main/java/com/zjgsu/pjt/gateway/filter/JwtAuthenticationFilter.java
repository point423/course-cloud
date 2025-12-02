package com.zjgsu.pjt.gateway.filter;

import com.zjgsu.pjt.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.util.AntPathMatcher; // 【1. 导入 AntPathMatcher】


import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // 白名单路径，这些路径的请求不需要JWT认证
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login"
    );
    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 【3. 使用 pathMatcher 进行更精确的匹配】
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, path)) {
                // 如果路径匹配白名单，直接放行
                return chain.filter(exchange);
            }
        }

        // 2. 从请求头获取Token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // 3. 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 4. 解析Token，获取用户信息
        Claims claims = jwtUtil.parseToken(token);
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 5. 将用户信息添加到请求头，转发给下游服务
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();

        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {
        // 设置过滤器的执行顺序，-100表示高优先级
        return -100;
    }
}