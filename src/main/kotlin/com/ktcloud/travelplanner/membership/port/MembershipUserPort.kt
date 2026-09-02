package com.ktcloud.travelplanner.membership.port

import java.util.UUID

/**
 * Membership가 Identity에 요청하는 최소 사용자 계약. 구현체는 HttpMembershipUserAdapter —
 * Identity의 GET /api/v1/users/lookup, GET /api/v1/users/{id}/summary 를 호출한다.
 *
 * 실패 시맨틱(SERVICE_COMMUNICATION_BOUNDARIES.md 3):
 *  - 404 = 정상적인 "없음/탈퇴" 결과 → null(reference) 또는 deleted=true(display)
 *  - 연결 실패·timeout·5xx = Identity 장애 → IdentityServiceUnavailableException(503)
 */
interface MembershipUserPort {
	/** 초대 대상 닉네임 조회. 없거나 탈퇴한 사용자면 null. */
	fun findByNickname(nickname: String): MembershipUserReference?

	/** 멤버/오너 표시용 조회. 탈퇴한 사용자면 deleted=true. */
	fun findDisplay(userId: UUID): MembershipUserDisplay
}

data class MembershipUserReference(val userId: UUID)

data class MembershipUserDisplay(
	val userId: UUID,
	val nickname: String?,
	val profileImageUrl: String?,
	val deleted: Boolean,
) {
	companion object {
		fun withdrawn(userId: UUID) = MembershipUserDisplay(userId, nickname = null, profileImageUrl = null, deleted = true)
	}
}
