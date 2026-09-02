package com.ktcloud.travelplanner.membership.service
import com.ktcloud.travelplanner.membership.model.InvitationStatus
import com.ktcloud.travelplanner.membership.model.TravelInvitationAction
import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelPermission
import com.ktcloud.travelplanner.membership.model.TravelRole
import com.ktcloud.travelplanner.membership.port.MembershipUserDisplay
import com.ktcloud.travelplanner.membership.port.MembershipUserPort
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.travel.model.Travel
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
class TravelMemberQueryServiceTest {
        private val travelRepository = mock(TravelRepository::class.java)
        private val travelMemberRepository = mock(TravelMemberRepository::class.java)
        private val membershipUserPort = mock(MembershipUserPort::class.java)
        private val service = TravelMemberQueryService(travelRepository, travelMemberRepository, membershipUserPort)

        init {
                for (id in listOf(OWNER_ID, READ_ONLY_ID, READ_WRITE_ID, OUTSIDER_ID)) {
                        `when`(membershipUserPort.findDisplay(id))
                                .thenReturn(MembershipUserDisplay(id, "nick-$id", null, deleted = false))
                }
        }

        @Test
        fun `owner and accepted members are combined with owner first`() {
                val travel = travel()
                val readOnlyMember = acceptedMember(travel, READ_ONLY_ID, TravelRole.READ_ONLY)
                val readWriteMember = acceptedMember(travel, READ_WRITE_ID, TravelRole.READ_WRITE)
                `when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
                `when`(travelMemberRepository.findVisibleMembers(TRAVEL_ID))
                        .thenReturn(listOf(readOnlyMember, readWriteMember))
                val response = service.getTravelMembers(TRAVEL_ID, OWNER_ID)
                assertEquals(listOf(OWNER_ID, READ_ONLY_ID, READ_WRITE_ID), response.map { it.userId })
                assertEquals(TravelPermission.OWNER, response[0].role)
                assertTrue(response[0].isOwner)
                assertEquals(InvitationStatus.ACCEPTED, response[0].status)
                assertEquals(TravelPermission.READ_ONLY, response[1].role)
                assertFalse(response[1].isOwner)
                assertEquals(InvitationStatus.ACCEPTED, response[1].status)
                assertEquals(TravelPermission.READ_WRITE, response[2].role)
                verify(travelMemberRepository, never()).findAcceptedRole(TRAVEL_ID, OWNER_ID)
        }
        @Test
        fun `pending invitation is included with pending status`() {
                val travel = travel()
                val pendingMember = pendingMember(travel, READ_ONLY_ID, TravelRole.READ_ONLY)
                `when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
                `when`(travelMemberRepository.findVisibleMembers(TRAVEL_ID)).thenReturn(listOf(pendingMember))
                val response = service.getTravelMembers(TRAVEL_ID, OWNER_ID)
                assertEquals(listOf(OWNER_ID, READ_ONLY_ID), response.map { it.userId })
                assertEquals(InvitationStatus.PENDING, response[1].status)
                assertEquals(null, response[0].invitationId)
                assertEquals(pendingMember.id, response[1].invitationId)
        }
        @Test
        fun `accepted read only or read write member can query list`() {
                val travel = travel()
                `when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
                `when`(travelMemberRepository.findAcceptedRole(TRAVEL_ID, READ_ONLY_ID)).thenReturn(TravelRole.READ_ONLY)
                `when`(travelMemberRepository.findVisibleMembers(TRAVEL_ID)).thenReturn(emptyList())
                val response = service.getTravelMembers(TRAVEL_ID, READ_ONLY_ID)
                assertEquals(1, response.size)
                verify(travelMemberRepository).findVisibleMembers(TRAVEL_ID)
        }
        @Test
        fun `user without accepted membership cannot query list`() {
                val travel = travel()
                `when`(travelRepository.findById(TRAVEL_ID)).thenReturn(Optional.of(travel))
                `when`(travelMemberRepository.findAcceptedRole(TRAVEL_ID, OUTSIDER_ID)).thenReturn(null)
                assertThrows<TravelMemberAccessDeniedException> {
                        service.getTravelMembers(TRAVEL_ID, OUTSIDER_ID)
                }
                verify(travelMemberRepository, never()).findVisibleMembers(TRAVEL_ID)
        }

        private fun acceptedMember(
                travel: Travel,
                userId: UUID,
                role: TravelRole,
        ): TravelMember = TravelMember(
                travel = travel,
                userId = userId,
                role = role,
                invitedAt = INVITED_AT,
        ).also {
                it.respond(TravelInvitationAction.ACCEPT, INVITED_AT.plusSeconds(1))
        }
        private fun pendingMember(
                travel: Travel,
                userId: UUID,
                role: TravelRole,
        ): TravelMember = TravelMember(
                travel = travel,
                userId = userId,
                role = role,
                invitedAt = INVITED_AT,
        )
        private fun travel(ownerId: UUID = OWNER_ID): Travel = Travel(
                id = TRAVEL_ID,
                ownerId = ownerId,
                title = "참여자 여행",
                startDate = LocalDate.parse("2026-08-01"),
                endDate = LocalDate.parse("2026-08-02"),
        )
        companion object {
                private val TRAVEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000032")
                private val OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")
                private val READ_ONLY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")
                private val READ_WRITE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003")
                private val OUTSIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004")
                private val INVITED_AT = Instant.parse("2026-07-01T00:00:00Z")
        }
}
