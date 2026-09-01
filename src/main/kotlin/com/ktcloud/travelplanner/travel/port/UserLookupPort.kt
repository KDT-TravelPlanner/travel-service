package com.ktcloud.travelplanner.travel.port

import com.ktcloud.travelplanner.user.model.User
import java.util.UUID

/** Identity 사용자 조회 계약. 구현체 교체를 위해 서비스 계층에서 Repository를 숨긴다. */
interface UserLookupPort {
    fun findById(id: UUID): User?
    fun findByNickname(nickname: String): User?
    fun existsById(id: UUID): Boolean
    fun findOwnerDisplayById(id: UUID): OwnerDisplay?
}

data class OwnerDisplay(
    val nickname: String?,
    val profileImageUrl: String?,
    val deletedAt: java.time.Instant?,
)
