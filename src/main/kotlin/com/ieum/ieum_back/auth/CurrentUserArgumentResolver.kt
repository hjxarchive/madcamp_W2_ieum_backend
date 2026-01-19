package com.ieum.ieum_back.auth

import com.ieum.ieum_back.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.*

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentUser::class.java) &&
                parameter.parameterType == UUID::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): UUID {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw UnauthorizedException("Invalid request")

        // 인터셉터에서 저장한 userId 가져오기
        val userId = request.getAttribute("userId") as? UUID

        // JWT 인터셉터를 거치지 않은 경우 (excludePathPatterns), X-User-Id 헤더 확인
        if (userId == null) {
            val headerUserId = request.getHeader("X-User-Id")
                ?: throw UnauthorizedException("User not authenticated")
            return UUID.fromString(headerUserId)
        }

        return userId
    }
}
