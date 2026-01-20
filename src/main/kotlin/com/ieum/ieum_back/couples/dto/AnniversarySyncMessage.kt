package com.ieum.ieum_back.couples.dto

import java.time.LocalDate

/**
 * 기념일 WebSocket 실시간 동기화 메시지
 */
data class AnniversarySyncMessage(
    val eventType: String,  // ANNIVERSARY_UPDATED
    val anniversary: AnniversaryDto?,
    val userId: String,
    val timestamp: String
)

data class AnniversaryDto(
    val date: String?  // LocalDate를 문자열로 전송
) {
    companion object {
        fun from(anniversary: LocalDate?): AnniversaryDto {
            return AnniversaryDto(
                date = anniversary?.toString()
            )
        }
    }
}
