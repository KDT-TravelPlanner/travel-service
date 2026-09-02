package com.ktcloud.travelplanner.testsupport

import com.ktcloud.travelplanner.membership.port.MembershipUserDisplay
import com.ktcloud.travelplanner.membership.port.MembershipUserPort
import com.ktcloud.travelplanner.membership.port.MembershipUserReference
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// 통합 테스트가 Identity HTTP 호출 없이 Membership 표시/닉네임 조회를 검증할 수 있게 하는 대역.
// 기본 동작: 등록되지 않은 nickname은 없음(null), 모든 userId는 "nick-<앞4자리>"로 표시,
// markWithdrawn한 userId는 deleted=true.
class FakeMembershipUserPort : MembershipUserPort {
	private val byNickname = ConcurrentHashMap<String, UUID>()
	private val nicknameById = ConcurrentHashMap<UUID, String>()
	private val withdrawn = ConcurrentHashMap.newKeySet<UUID>()

	fun register(nickname: String, userId: UUID) {
		byNickname[nickname] = userId
		nicknameById[userId] = nickname
	}

	fun markWithdrawn(userId: UUID) {
		withdrawn.add(userId)
	}

	fun reset() {
		byNickname.clear()
		nicknameById.clear()
		withdrawn.clear()
	}

	override fun findByNickname(nickname: String): MembershipUserReference? =
		byNickname[nickname]?.let { MembershipUserReference(it) }

	override fun findDisplay(userId: UUID): MembershipUserDisplay =
		if (userId in withdrawn) {
			MembershipUserDisplay.withdrawn(userId)
		} else {
			MembershipUserDisplay(
				userId = userId,
				nickname = nicknameById[userId] ?: "nick-${userId.toString().take(4)}",
				profileImageUrl = null,
				deleted = false,
			)
		}
}

@TestConfiguration(proxyBeanMethods = false)
class FakeMembershipUserPortConfig {
	@Bean
	@Primary
	fun fakeMembershipUserPort(): FakeMembershipUserPort = FakeMembershipUserPort()
}
