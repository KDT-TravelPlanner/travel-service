package com.ktcloud.travelplanner.global.exception

// HttpMapsAdapter가 Maps 서비스 호출에 실패(타임아웃/5xx/연결 실패 등)했을 때 던진다.
// SERVICE_COMMUNICATION_BOUNDARIES.md 4절: 경로 계산은 장애 시 503으로 실패한다
// (장소 좌표 조회는 별개 — 그쪽은 핀 하나 누락으로 완화한다).
class MapsServiceUnavailableException(
	cause: Throwable? = null,
) : ExternalServiceException(ErrorCode.MAPS_SERVICE_UNAVAILABLE, cause = cause)
