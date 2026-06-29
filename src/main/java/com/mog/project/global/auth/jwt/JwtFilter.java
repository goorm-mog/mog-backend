package com.mog.project.global.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// OncePerRequestFilter: 요청당 딱 한 번만 실행되는 필터
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // Authorization 헤더에서 토큰 추출 (Bearer xxx 형식)
        String token = resolveToken(request);

        // 토큰이 유효하면 SecurityContext에 인증 정보 저장
        if (token != null && jwtProvider.isValid(token)) {
            String userId = jwtProvider.getUserId(token);

            // Spring Security가 이 요청을 인증된 요청으로 인식하게 함
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        // "Bearer " 접두사 제거 후 토큰만 반환
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
