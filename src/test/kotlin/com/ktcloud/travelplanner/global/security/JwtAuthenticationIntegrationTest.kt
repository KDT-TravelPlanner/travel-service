package com.ktcloud.travelplanner.global.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.ktcloud.travelplanner.global.logging.RequestIdGenerator
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, JwtTestEndpointConfiguration::class)
@Transactional
class JwtAuthenticationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jwtTokenService: JwtTokenService,
	@Autowired private val entityManager: EntityManager,
	@Autowired private val objectMapper: ObjectMapper,
) {
	@Test
	fun `protected API returns common 401 without token`() {
		val response = mockMvc.get("/api/test/security/principal") {
			header(RequestIdGenerator.HEADER_NAME, "jwt-missing")
		}
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.code", equalTo("UNAUTHORIZED"))
			}
			.andReturn()
			.response

		val responseRequestId = response.getHeader(RequestIdGenerator.HEADER_NAME)
		UUID.fromString(responseRequestId)
		assertNotEquals("jwt-missing", responseRequestId)
		assertEquals(responseRequestId, objectMapper.readTree(response.contentAsString).path("requestId").asText())
	}

	@Test
	fun `valid access token injects authenticated user principal`() {
		val userId = saveUser()
		val token = jwtTokenService.issueAccessToken(userId).value

		mockMvc.get("/api/test/security/principal") {
			header("Authorization", "Bearer $token")
		}
			.andExpect {
				status { isOk() }
				jsonPath("$.userId", equalTo(userId.toString()))
			}
	}

	@Test
	fun `active token without required authority returns common 403`() {
		val userId = saveUser()
		val token = jwtTokenService.issueAccessToken(userId).value

		val response = mockMvc.get("/api/test/security/admin") {
			header(RequestIdGenerator.HEADER_NAME, "jwt-forbidden")
			header("Authorization", "Bearer $token")
		}
			.andExpect {
				status { isForbidden() }
				jsonPath("$.code", equalTo("ACCESS_DENIED"))
			}
			.andReturn()
			.response

		val responseRequestId = response.getHeader(RequestIdGenerator.HEADER_NAME)
		UUID.fromString(responseRequestId)
		assertNotEquals("jwt-forbidden", responseRequestId)
		assertEquals(responseRequestId, objectMapper.readTree(response.contentAsString).path("requestId").asText())
	}

	// Identity 분리 후 travel 필터는 DB 존재 확인을 하지 않는다(서명 검증만) — 탈퇴 사용자가
	// 만료 전 토큰으로 잠시 더 접근 가능해지는 트레이드오프는 PoC 스코프에서 허용한다
	// (AUTH_FORWARDING_CONTRACT.md). 따라서 "탈퇴 사용자 토큰 거부" 테스트는 더 이상 없다.

	@Test
	fun `malformed token is rejected as unauthorized`() {
		mockMvc.get("/api/test/security/principal") {
			header("Authorization", "Bearer not.a.valid.token")
		}
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.code", equalTo("UNAUTHORIZED"))
			}
	}

	@Test
	fun `CORS preflight remains available without authentication`() {
		mockMvc.options("/api/test/security/principal") {
			header("Origin", "http://localhost:3000")
			header("Access-Control-Request-Method", "GET")
		}
			.andExpect {
				status { isOk() }
				header { string("Access-Control-Allow-Origin", "http://localhost:3000") }
			}
	}

	private fun saveUser(): UUID = UUID.randomUUID()
}

data class AuthenticatedUserResponse(
	val userId: String,
)

@RestController
@RequestMapping("/api/test/security")
class JwtTestController {
	@GetMapping("/principal")
	fun principal(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
	): AuthenticatedUserResponse = AuthenticatedUserResponse(principal.userId.toString())

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	fun admin(): String = "admin"
}

@TestConfiguration(proxyBeanMethods = false)
class JwtTestEndpointConfiguration {
	@Bean
	fun jwtTestController(): JwtTestController = JwtTestController()
}
