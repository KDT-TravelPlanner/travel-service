package com.ktcloud.travelplanner.membership.adapter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ktcloud.travelplanner.global.exception.IdentityServiceUnavailableException
import com.ktcloud.travelplanner.global.external.applyIncomingRequestContext
import com.ktcloud.travelplanner.membership.port.MembershipUserDisplay
import com.ktcloud.travelplanner.membership.port.MembershipUserPort
import com.ktcloud.travelplanner.membership.port.MembershipUserReference
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

// MembershipUserPort의 HTTP 구현체. Identity가 프로세스로 분리된 뒤 travel이 User 테이블을 갖지
// 않으므로, 초대 대상 닉네임 조회와 멤버/오너 표시를 Identity API로 가져온다.
// 들어온 요청의 Authorization/X-Request-Id를 그대로 전달한다(applyIncomingRequestContext).
//
// 404는 정상적인 도메인 결과(없음/탈퇴)로 처리하고, 연결 실패·timeout·5xx는
// IdentityServiceUnavailableException(503)으로 올린다.
@Component
class HttpMembershipUserAdapter(
	@Qualifier("identityRestClient")
	private val identityRestClient: RestClient,
) : MembershipUserPort {
	override fun findByNickname(nickname: String): MembershipUserReference? {
		val body = try {
			identityRestClient.get()
				.uri { builder -> builder.path("/api/v1/users/lookup").queryParam("nickname", nickname).build() }
				.headers { it.applyIncomingRequestContext() }
				.retrieve()
				.body(UserSummaryEnvelope::class.java)
		} catch (exception: HttpClientErrorException.NotFound) {
			return null
		} catch (exception: RestClientException) {
			throw IdentityServiceUnavailableException(exception)
		}
		return body?.data?.let { MembershipUserReference(it.userId) }
	}

	override fun findDisplay(userId: UUID): MembershipUserDisplay {
		val body = try {
			identityRestClient.get()
				.uri("/api/v1/users/{userId}/summary", userId)
				.headers { it.applyIncomingRequestContext() }
				.retrieve()
				.body(UserSummaryEnvelope::class.java)
		} catch (exception: HttpClientErrorException.NotFound) {
			// 존재하지 않거나 탈퇴한 사용자 (Identity의 @SQLRestriction으로 탈퇴자는 404).
			return MembershipUserDisplay.withdrawn(userId)
		} catch (exception: RestClientException) {
			throw IdentityServiceUnavailableException(exception)
		}
		val data = body?.data ?: throw IdentityServiceUnavailableException()
		return MembershipUserDisplay(
			userId = data.userId,
			nickname = data.nickname,
			profileImageUrl = data.profileImageUrl,
			deleted = false,
		)
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UserSummaryEnvelope(
	val data: UserSummary?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UserSummary(
	val userId: UUID,
	val nickname: String?,
	val profileImageUrl: String?,
)
