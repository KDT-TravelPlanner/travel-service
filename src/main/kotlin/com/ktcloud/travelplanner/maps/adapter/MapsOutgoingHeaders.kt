package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.common.logging.RequestId
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// Travel이 Maps(`/internal/v1/maps/**`)를 호출할 때 들어온 사용자 요청에서 그대로 이어 넘겨야 하는
// 값을 모아둔다. Maps는 `/internal/**`을 `.authenticated()`로 막아두므로(모든 서비스가 같은
// JWT_SECRET을 공유, AUTH_FORWARDING_CONTRACT.md) Authorization을 전달하지 않으면 401을 받는다.
// X-Request-Id도 함께 넘겨 두 서비스 로그가 하나의 요청으로 묶이게 한다.
//
// 서비스 간 호출은 항상 사용자 요청 스레드에서 일어난다(비동기 경로 없음). RequestContextHolder가
// 비어 있으면(예: 스케줄러) 아무 헤더도 붙이지 않는다.

private fun currentRequest(): HttpServletRequest? =
	(RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

internal fun HttpHeaders.applyIncomingRequestContext() {
	val request = currentRequest() ?: return
	request.getHeader(HttpHeaders.AUTHORIZATION)?.let { set(HttpHeaders.AUTHORIZATION, it) }
	RequestLoggingContext.getRequestId(request)?.let { set(RequestId.HEADER_NAME, it) }
}
