package com.license.server.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter implements Filter {

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        if (!path.startsWith("/admin/") || path.startsWith("/admin/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(res, 401, "未登录");
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtProvider.parseToken(token);
            if (!"access".equals(claims.get("type"))) {
                sendError(res, 401, "无效的token类型");
                return;
            }
            req.setAttribute("userId", Long.parseLong(claims.getSubject()));
            req.setAttribute("username", claims.get("username"));
            chain.doFilter(request, response);
        } catch (Exception e) {
            sendError(res, 401, "token无效或已过期");
        }
    }

    private void sendError(HttpServletResponse res, int code, String message) throws IOException {
        res.setStatus(code);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":" + code + ",\"data\":null,\"message\":\"" + message + "\"}");
    }
}
