package com.ktcloud.travelplanner.membership.adapter

import com.ktcloud.travelplanner.common.logging.RequestId
import com.ktcloud.travelplanner.global.exception.IdentityServiceUnavailableException
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Identity 조회의 실패 시맨틱을 고정한다:
//   findByNickname: 404 -> null, 5xx -> IdentityServiceUnavailableException(503)
//   findDisplay:    404 -> deleted=true, 5xx -> 503, 200 본문 깨짐 -> 503
// 그리고 Authorization / X-Request-Id 를 그대로 전달하는지도 확인한다.
class HttpMembershipUserAdapterTest {
	private val userId: UUID = UUID.randomUUID()

	@AfterEach
	fun clear() = RequestContextHolder.resetRequestAttributes()

	private fun adapter(server: (MockRestServiceServer) -> Unit): HttpMembershipUserAdapter {
		val builder = RestClient.builder().baseUrl(BASE_URL)
		val mock = MockRestServiceServer.bindTo(builder).build()
		server(mock)
		return HttpMembershipUserAdapter(builder.build())
	}

	private fun givenIncomingRequest(authorization: String?, requestId: String?) {
		val request = MockHttpServletRequest()
		authorization?.let { request.addHeader(HttpHeaders.AUTHORIZATION, it) }
		requestId?.let { RequestLoggingContext.setRequestId(request, it) }
		RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
	}

	@Test
	fun `findByNickname returns the user id and forwards auth and request id`() {
		givenIncomingRequest("Bearer abc", REQUEST_ID)
		val a = adapter {
			it.expect(requestTo("$BASE_URL/api/v1/users/lookup?nickname=alice"))
				.andExpect(queryParam("nickname", "alice"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
				.andExpect(header(RequestId.HEADER_NAME, REQUEST_ID))
				.andRespond(withSuccess("""{"data":{"userId":"$userId","nickname":"alice","profileImageUrl":null}}""", MediaType.APPLICATION_JSON))
		}
		assertEquals(userId, a.findByNickname("alice")?.userId)
	}

	@Test
	fun `findByNickname returns null on 404`() {
		val a = adapter { it.expect(requestTo("$BASE_URL/api/v1/users/lookup?nickname=ghost")).andRespond(withStatus(HttpStatus.NOT_FOUND)) }
		assertNull(a.findByNickname("ghost"))
	}

	@Test
	fun `findByNickname throws IdentityServiceUnavailable on 5xx`() {
		val a = adapter { it.expect(requestTo("$BASE_URL/api/v1/users/lookup?nickname=x")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)) }
		assertThrows<IdentityServiceUnavailableException> { a.findByNickname("x") }
	}

	@Test
	fun `findDisplay returns the summary`() {
		val a = adapter {
			it.expect(requestTo("$BASE_URL/api/v1/users/$userId/summary"))
				.andRespond(withSuccess("""{"data":{"userId":"$userId","nickname":"bob","profileImageUrl":"http://img"}}""", MediaType.APPLICATION_JSON))
		}
		val display = a.findDisplay(userId)
		assertEquals("bob", display.nickname)
		assertEquals("http://img", display.profileImageUrl)
		assertEquals(false, display.deleted)
	}

	@Test
	fun `findDisplay maps 404 to a withdrawn user`() {
		val a = adapter { it.expect(requestTo("$BASE_URL/api/v1/users/$userId/summary")).andRespond(withStatus(HttpStatus.NOT_FOUND)) }
		val display = a.findDisplay(userId)
		assertTrue(display.deleted)
		assertNull(display.nickname)
	}

	@Test
	fun `findDisplay throws IdentityServiceUnavailable on 5xx`() {
		val a = adapter { it.expect(requestTo("$BASE_URL/api/v1/users/$userId/summary")).andRespond(withStatus(HttpStatus.BAD_GATEWAY)) }
		assertThrows<IdentityServiceUnavailableException> { a.findDisplay(userId) }
	}

	@Test
	fun `findDisplay throws when a 200 body is not the expected shape`() {
		val a = adapter {
			it.expect(requestTo("$BASE_URL/api/v1/users/$userId/summary"))
				.andRespond(withSuccess("""{"data":null}""", MediaType.APPLICATION_JSON))
		}
		assertThrows<IdentityServiceUnavailableException> { a.findDisplay(userId) }
	}

	@Test
	fun `no incoming context means no forwarded headers`() {
		givenIncomingRequest(authorization = null, requestId = null)
		val a = adapter {
			it.expect(requestTo("$BASE_URL/api/v1/users/$userId/summary"))
				.andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
				.andExpect(headerDoesNotExist(RequestId.HEADER_NAME))
				.andRespond(withSuccess("""{"data":{"userId":"$userId","nickname":"x","profileImageUrl":null}}""", MediaType.APPLICATION_JSON))
		}
		a.findDisplay(userId)
	}

	private companion object {
		const val BASE_URL = "http://identity-service:8080"
		const val REQUEST_ID = "11111111-2222-3333-4444-555555555555"
	}
}
