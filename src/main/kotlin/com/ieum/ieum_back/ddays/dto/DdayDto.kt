package com.ieum.ieum_back.ddays.dto

import java.time.LocalDate

data class DdayResponse(
    val title: String,
    val date: LocalDate,
    val dday: Long, // D-day (음수: 지남, 0: 오늘, 양수: 남음)
    val type: String // "anniversary", "event"
)

data class DdayListResponse(
    val ddays: List<DdayResponse>
)
