package com.mog.project.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtFilterTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_TOKEN_EXPIRY = 1000 * 60 * 30L;
    private static final long REFRESH_TOKEN_EXPIRY = 1000 * 60 * 60 * 24 * 7L;

    private JwtProvider jwtProvider;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY);
        jwtFilter = new JwtFilter(jwtProvider);
        // 각 테스트 전 SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_JWT가_있으면_SecurityContext에_인증정보가_저장된다() throws Exception {
        String token = jwtProvider.generateToken("123");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없으면_SecurityContext가_비어있다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 변조된_JWT면_SecurityContext가_비어있다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.value");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
