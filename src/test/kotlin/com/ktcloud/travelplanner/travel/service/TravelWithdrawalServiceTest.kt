package com.ktcloud.travelplanner.travel.service

import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelRole
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.travel.model.Travel
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import com.ktcloud.travelplanner.user.model.OAuthProvider
import com.ktcloud.travelplanner.user.model.User
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertSame

class TravelWithdrawalServiceTest {
	private val travelRepository = mock(TravelRepository::class.java)
	private val travelMemberRepository = mock(TravelMemberRepository::class.java)
	private val service = TravelWithdrawalService(travelRepository, travelMemberRepository)

	private val withdrawingId: UUID = UUID.randomUUID()
	private val owner = User(OAuthProvider.GOOGLE, "svc-owner")

	private fun travel(): Travel =
		Travel(owner = owner, title = "여행", startDate = LocalDate.parse("2026-08-01"), endDate = LocalDate.parse("2026-08-03"))

	private fun member(travel: Travel, user: User): TravelMember =
		TravelMember(travel = travel, user = user, role = TravelRole.READ_WRITE, invitedAt = Instant.parse("2026-01-01T00:00:00Z"))

	@Test
	fun `owned travel with a candidate transfers ownership and removes that member`() {
		val travel = travel()
		val candidateUser = User(OAuthProvider.GOOGLE, "svc-candidate")
		val candidate = member(travel, candidateUser)
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(travel))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(travel.id)).thenReturn(listOf(candidate))

		service.cleanUpOwnership(withdrawingId)

		assertSame(candidateUser, travel.owner)
		verify(travelMemberRepository).delete(candidate)
	}

	@Test
	fun `owned travel with no candidate is left untouched`() {
		val travel = travel()
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(travel))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(travel.id)).thenReturn(emptyList())

		service.cleanUpOwnership(withdrawingId)

		assertSame(owner, travel.owner)
		verify(travelMemberRepository, never()).delete(any())
	}

	@Test
	fun `no owned travels is a no-op`() {
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(emptyList())

		service.cleanUpOwnership(withdrawingId)

		verifyNoInteractions(travelMemberRepository)
	}

	@Test
	fun `mixed travels - only the one with a candidate is transferred`() {
		val withCandidate = travel()
		val withoutCandidate = travel()
		val candidateUser = User(OAuthProvider.GOOGLE, "svc-candidate")
		val candidate = member(withCandidate, candidateUser)
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(withCandidate, withoutCandidate))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(withCandidate.id)).thenReturn(listOf(candidate))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(withoutCandidate.id)).thenReturn(emptyList())

		service.cleanUpOwnership(withdrawingId)

		assertSame(candidateUser, withCandidate.owner)
		assertSame(owner, withoutCandidate.owner)
		verify(travelMemberRepository).delete(candidate)
	}
}
