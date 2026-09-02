package com.ktcloud.travelplanner.travel.controller

import com.ktcloud.travelplanner.global.security.JwtTokenService
import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelRole
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import com.ktcloud.travelplanner.travel.model.Travel
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// MSA 전환용 내부 API POST /api/v1/internal/users/{userId}/withdrawal 계약 회귀 테스트.
// Identity의 회원 탈퇴가 탈퇴 당사자의 JWT를 forward해서 호출한다.
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class InternalUserWithdrawalControllerIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val travelRepository: TravelRepository,
	@Autowired private val travelMemberRepository: TravelMemberRepository,
	@Autowired private val jwtTokenService: JwtTokenService,
	@Autowired private val jdbcTemplate: JdbcTemplate,
	@Autowired private val entityManager: EntityManager,
) {
	@Test
	fun `transfers ownership to a member and responds 204`() {
		val owner = saveUser("wd-owner")
		val member = saveUser("wd-member")
		val travel = saveTravel(owner)
		acceptMember(travel, member, TravelRole.READ_WRITE)

		withdraw(owner).andExpect { status { isNoContent() } }

		entityManager.flush()
		entityManager.clear()
		val reloaded = travelRepository.findById(travel.id).orElseThrow()
		assertOwner(reloaded, member)
		assertTrue(travelMemberRepository.findOwnershipTransferCandidates(travel.id).isEmpty())
	}

	@Test
	fun `owner with no members keeps ownership and still responds 204`() {
		val owner = saveUser("wd-lonely-owner")
		val travel = saveTravel(owner)

		withdraw(owner).andExpect { status { isNoContent() } }

		entityManager.flush()
		entityManager.clear()
		assertOwner(travelRepository.findById(travel.id).orElseThrow(), owner)
	}

	@Test
	fun `is idempotent - a second call is still 204`() {
		val owner = saveUser("wd-idem-owner")
		val member = saveUser("wd-idem-member")
		val travel = saveTravel(owner)
		acceptMember(travel, member, TravelRole.READ_WRITE)

		withdraw(owner).andExpect { status { isNoContent() } }
		withdraw(owner).andExpect { status { isNoContent() } }
	}

	@Test
	fun `rejects when the token subject does not match the path user`() {
		val owner = saveUser("wd-mismatch-owner")
		val other = saveUser("wd-mismatch-other")

		mockMvc.post("/api/v1/internal/users/${other}/withdrawal") {
			header(HttpHeaders.AUTHORIZATION, bearer(owner))
		}.andExpect { status { isForbidden() } }
	}

	@Test
	fun `unauthenticated request is rejected`() {
		mockMvc.post("/api/v1/internal/users/${UUID.randomUUID()}/withdrawal")
			.andExpect { status { isUnauthorized() } }
	}

	private fun withdraw(userId: UUID) = mockMvc.post("/api/v1/internal/users/$userId/withdrawal") {
		header(HttpHeaders.AUTHORIZATION, bearer(userId))
	}

	private fun assertOwner(travel: Travel, expected: UUID) =
		assertEquals(expected, travel.ownerId)

	private fun acceptMember(travel: Travel, userId: UUID, role: TravelRole) {
		val member = travelMemberRepository.saveAndFlush(
			TravelMember(travel = travel, userId = userId, role = role, invitedAt = Instant.parse("2026-01-01T00:00:00Z")),
		)
		jdbcTemplate.update("UPDATE planner_members SET status = 'ACCEPTED', responded_at = NOW() WHERE id = ?", member.id)
		entityManager.clear()
	}

	private fun bearer(userId: UUID): String =
		"Bearer ${jwtTokenService.issueAccessToken(userId).value}"

	private fun saveUser(nickname: String): UUID = UUID.randomUUID()

	private fun saveTravel(ownerId: UUID): Travel = travelRepository.saveAndFlush(
		Travel(ownerId = ownerId, title = "탈퇴 정리 여행", startDate = LocalDate.parse("2026-08-01"), endDate = LocalDate.parse("2026-08-03")),
	)
}
