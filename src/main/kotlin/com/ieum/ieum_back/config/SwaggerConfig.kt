package com.ieum.ieum_back.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "Bearer Authentication"
        
        return OpenAPI()
            .info(
                Info()
                    .title("이음(Ieum) API 명세서")
                    .version("1.0.0")
                    .description("""
                        ## 커플을 위한 종합 관리 플랫폼 API
                        
                        ### 핵심 기능
                        - 🔐 Google OAuth 2.0 기반 소셜 로그인
                        - 💑 초대 코드 기반 커플 매칭
                        - 🔒 End-to-End 암호화 채팅
                        - 📅 공유 일정 및 기념일 관리
                        - 💰 커플 가계부 (수입/지출 추적)
                        - 🎯 버킷리스트 및 추억 저장
                        - 🧬 연애 스타일 MBTI 테스트 (36문항)
                        - 🔔 WebSocket 실시간 알림
                        
                        ### 인증 방법
                        1. `/api/auth/google` 엔드포인트로 Google ID Token 전송
                        2. 응답으로 받은 JWT AccessToken을 저장
                        3. 이후 모든 API 요청 시 Authorization 헤더에 포함
                        ```
                        Authorization: Bearer {accessToken}
                        ```
                    """.trimIndent())
            )
            .servers(
                listOf(
                    Server().url("http://54.66.195.91/api").description("Production Server (AWS EC2)"),
                    Server().url("http://localhost:8080/api").description("Local Development Server")
                )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Google OAuth 로그인 후 받은 JWT 토큰을 입력하세요")
                    )
            )
    }
}
