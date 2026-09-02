package com.ktcloud.travelplanner.travel.adapter

import com.ktcloud.travelplanner.travel.port.OwnerDisplay
import com.ktcloud.travelplanner.travel.port.UserLookupPort
import com.ktcloud.travelplanner.user.model.User
import com.ktcloud.travelplanner.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaUserLookupAdapter(
    private val userRepository: UserRepository,
) : UserLookupPort {
    override fun findById(id: UUID): User? = userRepository.findById(id).orElse(null)
    override fun findByNickname(nickname: String): User? = userRepository.findByNickname(nickname)
    override fun existsById(id: UUID): Boolean = userRepository.existsById(id)
    override fun findOwnerDisplayById(id: UUID): OwnerDisplay? =
        userRepository.findOwnerDisplayById(id)?.let {
            OwnerDisplay(it.getNickname(), it.getProfileImageUrl(), it.getDeletedAt())
        }
}
