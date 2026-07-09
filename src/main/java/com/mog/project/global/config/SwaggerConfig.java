package com.mog.project.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "bearerAuth";
        String kakaoScheme = "kakaoAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Mog API")
                .description("Mog 서비스 API 명세서")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList(jwtScheme))
            .components(new Components()
                .addSecuritySchemes(jwtScheme, new SecurityScheme()
                    .name(jwtScheme)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
                .addSecuritySchemes(kakaoScheme, new SecurityScheme()
                    .name(kakaoScheme)
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .authorizationUrl("https://kauth.kakao.com/oauth/authorize")
                            .tokenUrl("https://kauth.kakao.com/oauth/token")
                            .scopes(new Scopes()
                                .addString("profile_nickname", "닉네임")
                                .addString("account_email", "이메일"))))));
    }
}
