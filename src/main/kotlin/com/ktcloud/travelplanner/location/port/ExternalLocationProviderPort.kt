package com.ktcloud.travelplanner.location.port

/** 외부 위치 카탈로그·지도 제공자 연동을 위한 계약. 현재 구현체는 연결하지 않는다. */
interface ExternalLocationProviderPort {
    fun searchCountries(keyword: String): List<ExternalLocationResult>
    fun searchCities(countryCode: String, keyword: String): List<ExternalLocationResult>
}

data class ExternalLocationResult(
    val externalId: String,
    val name: String,
    val countryCode: String?,
)
