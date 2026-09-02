package com.ktcloud.travelplanner.membership.dto
import com.ktcloud.travelplanner.membership.model.InvitationStatus
import com.ktcloud.travelplanner.membership.model.TravelMember
import com.ktcloud.travelplanner.membership.model.TravelPermission
import com.ktcloud.travelplanner.membership.port.MembershipUserDisplay
import java.util.UUID
data class TravelMemberResponse(
        val userId: UUID,
        val nickname: String?,
        val profileImageUrl: String?,
        val role: TravelPermission,
        val isOwner: Boolean,
        val status: InvitationStatus,
        // 이슈 #232 — PENDING 멤버는 초대 취소(DELETE /invitations/{invitationId})로만 제거할 수 있고,
        // 그 API는 TravelMember row 자체의 id를 요구한다. 오너는 초대 row가 없어 null.
        val invitationId: UUID?,
) {
        companion object {
                private const val WITHDRAWN_USER_LABEL = "탈퇴한 사용자"

                // travel은 User 테이블을 갖지 않으므로 표시 정보는 Identity에서 조회한 값을 넘겨받는다.
                // 탈퇴한 사용자(display.deleted)는 닉네임을 "탈퇴한 사용자"로, 프로필 이미지를 null로 마스킹한다.
                fun fromOwner(display: MembershipUserDisplay): TravelMemberResponse = TravelMemberResponse(
                        userId = display.userId,
                        nickname = if (display.deleted) WITHDRAWN_USER_LABEL else display.nickname,
                        profileImageUrl = if (display.deleted) null else display.profileImageUrl,
                        role = TravelPermission.OWNER,
                        isOwner = true,
                        // 오너는 초대 개념이 없어 항상 ACCEPTED로 취급한다.
                        status = InvitationStatus.ACCEPTED,
                        invitationId = null,
                )
                fun fromMember(
                        member: TravelMember,
                        display: MembershipUserDisplay,
                ): TravelMemberResponse = TravelMemberResponse(
                        userId = member.userId,
                        nickname = if (display.deleted) WITHDRAWN_USER_LABEL else display.nickname,
                        profileImageUrl = if (display.deleted) null else display.profileImageUrl,
                        role = TravelPermission.valueOf(member.role.name),
                        isOwner = false,
                        status = member.status,
                        invitationId = member.id,
                )
        }
}
