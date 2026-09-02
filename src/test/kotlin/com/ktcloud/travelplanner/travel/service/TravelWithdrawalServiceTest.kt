package com.ktcloud.travelplanner.travel.service

import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelRole
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.travel.model.Travel
import com.ktcloud.travelplanner.travel.repository.TravelRepository
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
import kotlin.test.assertEquals

class TravelWithdrawalServiceTest {
	private val travelRepository = mock(TravelRepository::class.java)
	private val travelMemberRepository = mock(TravelMemberRepository::class.java)
	private val service = TravelWithdrawalService(travelRepository, travelMemberRepository)

	private val withdrawingId: UUID = UUID.randomUUID()

	private fun travel(): Travel =
		Travel(ownerId = withdrawingId, title = "여행", startDate = LocalDate.parse("2026-08-01"), endDate = LocalDate.parse("2026-08-03"))

	private fun member(travel: Travel, userId: UUID): TravelMember =
		TravelMember(travel = travel, userId = userId, role = TravelRole.READ_WRITE, invitedAt = Instant.parse("2026-01-01T00:00:00Z"))

	@Test
	fun `owned travel with a candidate transfers ownership and removes that member`() {
		val travel = travel()
		val candidateUserId = UUID.randomUUID()
		val candidate = member(travel, candidateUserId)
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(travel))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(travel.id)).thenReturn(listOf(candidate))

		service.cleanUpOwnership(withdrawingId)

		assertEquals(candidateUserId, travel.ownerId)
		verify(travelMemberRepository).delete(candidate)
	}

	@Test
	fun `owned travel with no candidate is left untouched`() {
		val travel = travel()
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(travel))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(travel.id)).thenReturn(emptyList())

		service.cleanUpOwnership(withdrawingId)

		assertEquals(withdrawingId, travel.ownerId)
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
		val candidateUserId = UUID.randomUUID()
		val candidate = member(withCandidate, candidateUserId)
		`when`(travelRepository.findAllByOwnerId(withdrawingId)).thenReturn(listOf(withCandidate, withoutCandidate))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(withCandidate.id)).thenReturn(listOf(candidate))
		`when`(travelMemberRepository.findOwnershipTransferCandidates(withoutCandidate.id)).thenReturn(emptyList())

		service.cleanUpOwnership(withdrawingId)

		assertEquals(candidateUserId, withCandidate.ownerId)
		assertEquals(withdrawingId, withoutCandidate.ownerId)
		verify(travelMemberRepository).delete(candidate)
	}
}
