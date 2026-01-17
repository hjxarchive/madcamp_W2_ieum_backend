package com.ieum.ieum_back.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/api/health")
    fun health(): String {
        return "이음(ieum) 서버가 정상적으로 응답하고 있습니다!"
    }
}