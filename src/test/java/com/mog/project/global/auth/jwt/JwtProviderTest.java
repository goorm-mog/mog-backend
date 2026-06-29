package com.mog.project.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;     
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {
    // 테스트 용 시크릿 키 (실제 앱에선 application.yaml에서 주입)
    private static final String SECRET = "local-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_TOKEN_EXPIRY = 1000 * 60 * 30L;   // 30분
    private static final long REFRESH_TOKEN_EXPIRY = 1000 * 60 * 60 * 24 * 7L; // 7일

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp(){
        jwtProvider = new JwtProvider(SECRET, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY);
    }

    @Test
    void 토큰을_생성하면_null이_아니다() {
        String token = jwtProvider.generateToken("123");
        assertThat(token).isNotNull();
    }

    @Test
    void 생성한_토큰에서_userId를_추출할수있다() {
        String token = jwtProvider.generateToken("123");
        String extractedId = jwtProvider.getUserId(token);
        assertThat(extractedId).isEqualTo("123");
    }
      @Test                                                     
      void 유효한_토큰이면_true를_반환한다() {
          String token = jwtProvider.generateToken("123");      
          assertThat(jwtProvider.isValid(token)).isTrue();
      }                                                         
                                          
      @Test                                                   
      void 변조된_토큰이면_false를_반환한다() {                 
          String token = jwtProvider.generateToken("123") + "tampered";                                                   
          assertThat(jwtProvider.isValid(token)).isFalse();
      }                                                       
                                                                
      @Test
      void 만료된_토큰이면_false를_반환한다() {                 
          // 만료시간 -1ms → 즉시 만료        
          JwtProvider expiredProvider = new JwtProvider(SECRET, -1L, REFRESH_TOKEN_EXPIRY);                                       
          String token = expiredProvider.generateToken("123");  
          assertThat(expiredProvider.isValid(token)).isFalse();
      }                                                         
  }             
