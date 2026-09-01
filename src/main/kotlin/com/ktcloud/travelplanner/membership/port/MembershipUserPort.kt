package com.ktcloud.travelplanner.membership.port

import java.util.UUID

/** Membership가 Identity에 요청하는 최소 사용자 계약. */
interface MembershipUserPort {
    fun findById(userId: UUID): MembershipUserReference?
    fun findByNickname(nickname: String): MembershipUserReference?
    fun findDisplay(userId: UUID): MembershipUserDisplay?
}

data class MembershipUserReference(val userId: UUID)

data class MembershipUserDisplay(
    val userId: UUID,
    val nickname: String?,
    val profileImageUrl: String?,
    val deleted: Boolean,
)
