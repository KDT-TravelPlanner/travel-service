package com.ktcloud.travelplanner.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

// Identity 분리 후 travel은 user_table을 갖지 않으므로 DB 존재 확인을 하지 않는다.
// 모든 서비스가 같은 HS256 JWT_SECRET을 공유하므로 서명 검증만으로 요청자를 신뢰한다
// (AUTH_FORWARDING_CONTRACT.md — community/maps 필터와 동일). 탈퇴 사용자가 만료 전 토큰으로
// 잠시 더 접근 가능해지는 트레이드오프는 이번 PoC 스코프에서 허용한다.
class JwtAuthenticationFilter(
	private val jwtTokenService: JwtTokenService,
	private val apiSecurityErrorHandler: ApiSecurityErrorHandler,
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val authorization = request.getHeader(AUTHORIZATION_HEADER)
		if (authorization == null) {
			filterChain.doFilter(request, response)
			return
		}

		try {
			val token = extractBearerToken(authorization)
			val userId = jwtTokenService.parseUserId(token)

			val principal = AuthenticatedUserPrincipal(userId)
			val authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				emptyList(),
			).apply {
				details = WebAuthenticationDetailsSource().buildDetails(request)
			}
			SecurityContextHolder.getContext().authentication = authentication
			filterChain.doFilter(request, response)
		} catch (exception: InvalidAccessTokenException) {
			SecurityContextHolder.clearContext()
			apiSecurityErrorHandler.commence(
				request,
				response,
				BadCredentialsException("Invalid bearer token.", exception),
			)
		}
	}

	private fun extractBearerToken(authorization: String): String {
		if (!authorization.startsWith(BEARER_PREFIX) || authorization.length == BEARER_PREFIX.length) {
			throw InvalidAccessTokenException()
		}
		val token = authorization.substring(BEARER_PREFIX.length)
		if (token.isBlank() || token.any(Char::isWhitespace)) {
			throw InvalidAccessTokenException()
		}
		return token
	}

	companion object {
		private const val AUTHORIZATION_HEADER = "Authorization"
		private const val BEARER_PREFIX = "Bearer "
	}
}
