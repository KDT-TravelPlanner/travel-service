package com.ktcloud.travelplanner.global.external

import com.ktcloud.travelplanner.common.logging.RequestId
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// Travel이 다른 서비스(Maps `/internal/**`, Identity `/api/v1/users/**`)를 호출할 때 들어온
// 사용자 요청에서 그대로 이어 넘겨야 하는 값을 모아둔다.
//   - Authorization: 대상 서비스가 같은 JWT_SECRET으로 직접 검증한다(AUTH_FORWARDING_CONTRACT.md).
//     안 넘기면 Maps `/internal/**`, Identity `/api/**` 모두 401.
//   - X-Request-Id: 두 서비스 로그가 하나의 사용자 요청으로 묶이게 한다.
//
// 서비스 간 호출은 항상 사용자 요청 스레드에서 일어난다(비동기 경로 없음). RequestContextHolder가
// 비어 있으면(예: 스케줄러) 아무 헤더도 붙이지 않는다.

private fun currentRequest(): HttpServletRequest? =
	(RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

fun HttpHeaders.applyIncomingRequestContext() {
	val request = currentRequest() ?: return
	request.getHeader(HttpHeaders.AUTHORIZATION)?.let { set(HttpHeaders.AUTHORIZATION, it) }
	RequestLoggingContext.getRequestId(request)?.let { set(RequestId.HEADER_NAME, it) }
}
