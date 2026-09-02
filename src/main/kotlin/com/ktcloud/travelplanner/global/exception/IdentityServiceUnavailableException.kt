package com.ktcloud.travelplanner.global.exception

// Identity 사용자 조회 HTTP 호출이 연결 실패·timeout·5xx로 실패했을 때 던진다.
// SERVICE_COMMUNICATION_BOUNDARIES.md 3: 초대 대상 확정이나 멤버 정보가 필요한 요청은 503.
// (404는 정상적인 "없음/탈퇴" 결과라 이 예외가 아니라 도메인 처리로 간다.)
class IdentityServiceUnavailableException(
	cause: Throwable? = null,
) : ExternalServiceException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE, cause = cause)
