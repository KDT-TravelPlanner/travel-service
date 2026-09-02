package com.ktcloud.travelplanner.travel.controller

import com.ktcloud.travelplanner.global.security.AuthenticatedUserPrincipal
import com.ktcloud.travelplanner.travel.service.TravelWithdrawalService
import com.ktcloud.travelplanner.travel.service.WithdrawalSubjectMismatchException
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// MSA 전환용 내부 API — Identity의 회원 탈퇴가 호출한다.
// 원 요청자(탈퇴 당사자)의 JWT를 그대로 forward받으므로, path의 userId와 토큰 주체가 같은지
// 확인한 뒤 Travel 소유의 소유권/멤버십 정리를 수행한다. 요청 본문 없음, 성공 시 204.
// 멱등(SERVICE_COMMUNICATION_BOUNDARIES.md 5) — 재시도돼도 성공으로 응답한다.
@RestController
@RequestMapping("/api/v1/internal/users")
class InternalUserWithdrawalController(
	private val travelWithdrawalService: TravelWithdrawalService,
) {
	@PostMapping("/{userId}/withdrawal")
	fun prepareWithdrawal(
		@PathVariable userId: UUID,
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
	): ResponseEntity<Unit> {
		if (principal.userId != userId) {
			throw WithdrawalSubjectMismatchException()
		}
		travelWithdrawalService.cleanUpOwnership(userId)
		return ResponseEntity.noContent().build()
	}
}
