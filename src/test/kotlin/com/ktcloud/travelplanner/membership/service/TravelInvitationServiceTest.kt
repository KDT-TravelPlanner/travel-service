package com.ktcloud.travelplanner.membership.service

import com.ktcloud.travelplanner.membership.dto.TravelInvitationCreateRequest
import com.ktcloud.travelplanner.membership.dto.TravelInvitationRespondRequest
import com.ktcloud.travelplanner.membership.model.InvitationStatus
import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelInvitationAction
import com.ktcloud.travelplanner.membership.model.TravelRole
import com.ktcloud.travelplanner.membership.port.MembershipUserDisplay
import com.ktcloud.travelplanner.membership.port.MembershipUserPort
import com.ktcloud.travelplanner.membership.port.MembershipUserReference
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.testsupport.TestFixtures
import com.ktcloud.travelplanner.travel.model.Travel
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class TravelInvitationServiceTest {
	private val travelRepository = mock(TravelRepository::class.java)
	private val travelMemberRepository = mock(TravelMemberRepository::class.java)
	private val membershipUserPort = mock(MembershipUserPort::class.java)
	private val service = TravelInvitationService(
		travelRepository,
		travelMemberRepository,
		membershipUserPort,
		TestFixtures.FIXED_CLOCK,
	)

	@Test
	fun `owner cancels pending invitation belonging to travel`() {
		val invitation = invitation()
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(invitation))

		service.cancelInvitation(TRAVEL_ID, INVITATION_ID, OWNER_ID)

		verify(travelMemberRepository).delete(invitation)
	}

	@Test
	fun `non owner or mismatched travel cannot cancel invitation`() {
		val invitation = invitation()
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(invitation))

		assertThrows<InvitationAccessDeniedException> {
			service.cancelInvitation(TRAVEL_ID, INVITATION_ID, INVITEE_ID)
		}
		assertThrows<TravelInvitationNotFoundException> {
			service.cancelInvitation(OTHER_TRAVEL_ID, INVITATION_ID, OWNER_ID)
		}

		verify(travelMemberRepository, never()).delete(invitation)
	}

	@Test
	fun `owner cannot cancel already answered invitation`() {
		val invitation = invitation().also {
			it.respond(TravelInvitationAction.ACCEPT, INVITED_AT.plusSeconds(1))
		}
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(invitation))

		assertThrows<InvitationAlreadyRespondedException> {
			service.cancelInvitation(TRAVEL_ID, INVITATION_ID, OWNER_ID)
		}

		verify(travelMemberRepository, never()).delete(invitation)
	}

	@Test
	fun `invitee accepts pending invitation and response time is recorded`() {
		val member = invitation()
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(member))
		`when`(travelMemberRepository.save(member)).thenReturn(member)

		val response = service.respondToInvitation(
			INVITATION_ID,
			INVITEE_ID,
			TravelInvitationRespondRequest(TravelInvitationAction.ACCEPT),
		)

		assertEquals(INVITATION_ID, response.invitationId)
		assertEquals(InvitationStatus.ACCEPTED, response.status)
		assertEquals(TestFixtures.FIXED_INSTANT, response.respondedAt)
	}

	@Test
	fun `invitee rejects pending invitation and row is deleted instead of saved`() {
		val member = invitation()
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(member))

		val response = service.respondToInvitation(
			INVITATION_ID,
			INVITEE_ID,
			TravelInvitationRespondRequest(TravelInvitationAction.REJECT),
		)

		assertEquals(INVITATION_ID, response.invitationId)
		assertEquals(InvitationStatus.REJECTED, response.status)
		assertEquals(TestFixtures.FIXED_INSTANT, response.respondedAt)
		verify(travelMemberRepository).delete(member)
		verify(travelMemberRepository, never()).save(member)
	}

	@Test
	fun `user other than invitee cannot respond to invitation`() {
		val member = invitation()
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(member))

		assertThrows<InvitationAccessDeniedException> {
			service.respondToInvitation(
				INVITATION_ID,
				OWNER_ID,
				TravelInvitationRespondRequest(TravelInvitationAction.REJECT),
			)
		}

		verify(travelMemberRepository, never()).save(member)
	}

	@Test
	fun `already answered invitation cannot transition again`() {
		val member = invitation().also {
			it.respond(TravelInvitationAction.REJECT, INVITED_AT.plusSeconds(1))
		}
		`when`(travelMemberRepository.findByIdForUpdate(INVITATION_ID)).thenReturn(Optional.of(member))

		assertThrows<InvitationAlreadyRespondedException> {
			service.respondToInvitation(
				INVITATION_ID,
				INVITEE_ID,
				TravelInvitationRespondRequest(TravelInvitationAction.ACCEPT),
			)
		}

		verify(travelMemberRepository, never()).save(member)
	}

	@Test
	fun `received invitations are queried by user and status and mapped with summaries`() {
		val member = TravelMember(
			id = INVITATION_ID,
			travel = travel(),
			userId = INVITEE_ID,
			role = TravelRole.READ_ONLY,
			invitedAt = INVITED_AT,
		)
		val pageable = PageRequest.of(1, 2)
		`when`(
			travelMemberRepository.findReceivedInvitations(
				INVITEE_ID,
				InvitationStatus.PENDING,
				pageable,
			),
		).thenReturn(PageImpl(listOf(member), pageable, 3))
		`when`(membershipUserPort.findDisplay(OWNER_ID))
			.thenReturn(MembershipUserDisplay(OWNER_ID, "owner", "https://example.com/owner.png", deleted = false))

		val response = service.getReceivedInvitations(
			INVITEE_ID,
			InvitationStatus.PENDING,
			page = 1,
			size = 2,
		)

		assertEquals(1, response.page)
		assertEquals(3, response.totalElements)
		val invitation = response.content.single()
		assertEquals(INVITATION_ID, invitation.invitationId)
		assertEquals(InvitationStatus.PENDING, invitation.status)
		assertEquals(TravelRole.READ_ONLY, invitation.role)
		assertEquals(TRAVEL_ID, invitation.travel.travelId)
		assertEquals("초대 여행", invitation.travel.title)
		assertEquals(OWNER_ID, invitation.inviter.userId)
		assertEquals("owner", invitation.inviter.nickname)
		assertEquals("https://example.com/owner.png", invitation.inviter.profileImageUrl)
	}

	@Test
	fun `received invitation status filter is passed to repository`() {
		val pageable = PageRequest.of(0, 20)
		`when`(
			travelMemberRepository.findReceivedInvitations(
				INVITEE_ID,
				InvitationStatus.REJECTED,
				pageable,
			),
		).thenReturn(PageImpl(emptyList(), pageable, 0))

		val response = service.getReceivedInvitations(
			INVITEE_ID,
			InvitationStatus.REJECTED,
			page = 0,
			size = 20,
		)

		assertEquals(emptyList(), response.content)
		verify(travelMemberRepository).findReceivedInvitations(
			INVITEE_ID,
			InvitationStatus.REJECTED,
			pageable,
		)
	}

	@Test
	fun `owner creates pending invitation for nickname with requested role`() {
		val travel = travel()
		lateinit var savedMember: TravelMember
		`when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
		`when`(membershipUserPort.findByNickname("invitee")).thenReturn(MembershipUserReference(INVITEE_ID))
		`when`(travelMemberRepository.existsByTravelAndUser(TRAVEL_ID, INVITEE_ID)).thenReturn(false)
		`when`(travelMemberRepository.save(any(TravelMember::class.java))).thenAnswer {
			it.getArgument<TravelMember>(0).also { member -> savedMember = member }
		}

		val response = service.createInvitation(TRAVEL_ID, OWNER_ID, request())

		assertEquals(savedMember.id, response.invitationId)
		assertEquals(travel.id, savedMember.travel.id)
		assertEquals(INVITEE_ID, savedMember.userId)
		assertEquals(TravelRole.READ_WRITE, savedMember.role)
		assertEquals(TestFixtures.FIXED_INSTANT, savedMember.invitedAt)
	}

	@Test
	fun `non owner cannot resolve target or create invitation`() {
		val travel = travel()
		`when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))

		assertThrows<InvitationAccessDeniedException> {
			service.createInvitation(TRAVEL_ID, INVITEE_ID, request())
		}

		verifyNoInteractions(membershipUserPort, travelMemberRepository)
	}

	@Test
	fun `owner cannot invite self or existing membership`() {
		val travel = travel()
		`when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
		`when`(membershipUserPort.findByNickname("invitee")).thenReturn(MembershipUserReference(OWNER_ID))

		assertThrows<SelfInvitationException> {
			service.createInvitation(TRAVEL_ID, OWNER_ID, request())
		}

		`when`(membershipUserPort.findByNickname("invitee")).thenReturn(MembershipUserReference(INVITEE_ID))
		`when`(travelMemberRepository.existsByTravelAndUser(TRAVEL_ID, INVITEE_ID)).thenReturn(true)

		assertThrows<DuplicateInvitationException> {
			service.createInvitation(TRAVEL_ID, OWNER_ID, request())
		}

		verify(travelMemberRepository, never()).save(any(TravelMember::class.java))
	}

	private fun request() = TravelInvitationCreateRequest("invitee", TravelRole.READ_WRITE)

	private fun invitation(): TravelMember = TravelMember(
		id = INVITATION_ID,
		travel = travel(),
		userId = INVITEE_ID,
		role = TravelRole.READ_WRITE,
		invitedAt = INVITED_AT,
	)

	private fun travel(ownerId: UUID = OWNER_ID): Travel = Travel(
		id = TRAVEL_ID,
		ownerId = ownerId,
		title = "초대 여행",
		startDate = LocalDate.parse("2026-08-01"),
		endDate = LocalDate.parse("2026-08-02"),
	)

	companion object {
		private val TRAVEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000028")
		private val OTHER_TRAVEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000030")
		private val INVITATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000029")
		private val OWNER_ID = TestFixtures.USER_ID
		private val INVITEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")
		private val INVITED_AT = Instant.parse("2026-07-01T00:00:00Z")
	}
}
