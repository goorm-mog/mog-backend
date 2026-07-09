package com.mog.project.domain.auth.controller;

import com.mog.project.domain.auth.dto.request.KakaoCodeLoginRequest;
import com.mog.project.domain.auth.dto.request.KakaoLoginRequest;
import com.mog.project.domain.auth.dto.response.AccessTokenReissueResponse;
import com.mog.project.domain.auth.dto.response.AuthLoginResponse;
import com.mog.project.domain.auth.service.AuthService;
import com.mog.project.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "카카오 로그인",
        description = "카카오 Access Token으로 자체 JWT를 발급합니다. Refresh Token은 HttpOnly Cookie로 설정됩니다.",
        security = {}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 카카오 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "카카오 서버 통신 실패")
    })
    @PostMapping("/login/kakao")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> kakaoLogin(
        @RequestBody KakaoLoginRequest request,
        HttpServletResponse response
    ) {
        AuthLoginResponse loginResponse = authService.kakaoLogin(request, response);
        return ResponseEntity.ok(ApiResponse.success(
            "AUTH_LOGIN_SUCCESS",
            "카카오 로그인에 성공하여 토큰이 발급되었습니다.",
            loginResponse
        ));
    }

    @Operation(
        summary = "카카오 로그인 (인가코드)",
        description = "카카오 인가코드(code)로 카카오 accessToken을 서버에서 교환한 뒤 자체 JWT를 발급합니다. Refresh Token은 HttpOnly Cookie로 설정됩니다.",
        security = {}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 카카오 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "카카오 서버 통신 실패")
    })
    @PostMapping("/login/kakao/code")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> kakaoLoginWithCode(
        @RequestBody KakaoCodeLoginRequest request,
        HttpServletResponse response
    ) {
        AuthLoginResponse loginResponse = authService.kakaoLoginWithCode(request.code(), response);
        return ResponseEntity.ok(ApiResponse.success(
            "AUTH_LOGIN_SUCCESS",
            "카카오 로그인에 성공하여 토큰이 발급되었습니다.",
            loginResponse
        ));
    }

    @Operation(
        summary = "Access Token 재발급",
        description = "쿠키의 Refresh Token으로 새 Access Token을 발급합니다. Refresh Token도 함께 교체됩니다(RTR).",
        security = {}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공",
            content = @Content(schema = @Schema(implementation = AccessTokenReissueResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 없음 또는 유효하지 않음")
    })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AccessTokenReissueResponse>> reissue(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        AccessTokenReissueResponse reissueResponse = authService.reissue(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success(
            "AUTH_REISSUE_SUCCESS",
            "Access Token이 재발급되었습니다.",
            reissueResponse
        ));
    }

    @Operation(
        summary = "로그아웃",
        description = "Redis에서 Refresh Token을 삭제하고 쿠키를 만료시킵니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success(
            "AUTH_LOGOUT_SUCCESS",
            "로그아웃에 성공하였습니다."
        ));
    }
}
