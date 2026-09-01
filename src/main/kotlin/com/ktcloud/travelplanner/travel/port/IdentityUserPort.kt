package com.ktcloud.travelplanner.travel.port

import java.time.Instant
import java.util.UUID

/** Travel 서비스가 Identity 서비스에 요구하는 사용자 계약. */
interface IdentityUserPort {
    fun findActiveUser(userId: UUID): TravelUserReference?
    fun findActiveUserByNickname(nickname: String): TravelUserReference?
    fun existsActiveUser(userId: UUID): Boolean
    fun findUserDisplay(userId: UUID): TravelUserDisplay?
}

data class TravelUserReference(val userId: UUID)

data class TravelUserDisplay(
    val userId: UUID,
    val nickname: String?,
    val profileImageUrl: String?,
    val deletedAt: Instant?,
)
