package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.common.logging.RequestId
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import com.ktcloud.travelplanner.route.model.TransportationType
import com.ktcloud.travelplanner.route.port.RouteCalculationWaypoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// Maps는 /internal/** 을 인증 필요로 막아둔다. Travel이 사용자 요청의 Authorization과
// X-Request-Id를 그대로 실어 보내는지 고정한다 — 빠지면 travel→maps 호출이 전부 401.
class MapsOutgoingHeadersTest {
	@AfterEach
	fun clearRequestContext() {
		RequestContextHolder.resetRequestAttributes()
	}

	private fun givenIncomingRequest(authorization: String?, requestId: String?) {
		val request = MockHttpServletRequest()
		authorization?.let { request.addHeader(HttpHeaders.AUTHORIZATION, it) }
		requestId?.let { RequestLoggingContext.setRequestId(request, it) }
		RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
	}

	private fun bind(): Pair<MockRestServiceServer, RestClient.Builder> {
		val builder = RestClient.builder().baseUrl(BASE_URL)
		return MockRestServiceServer.bindTo(builder).build() to builder
	}

	@Test
	fun `routes calculate 호출에 Authorization과 X-Request-Id를 그대로 싣는다`() {
		givenIncomingRequest(BEARER_TOKEN, INCOMING_REQUEST_ID)
		val (server, builder) = bind()
		server.expect(requestTo("$BASE_URL/internal/v1/maps/routes/calculate"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
			.andExpect(header(RequestId.HEADER_NAME, INCOMING_REQUEST_ID))
			.andRespond(
				withSuccess(
					"""{"encodedPolyline":null,"encodedPolylines":[],"totalDistanceMeters":0,"totalDurationSeconds":0,"legs":[],"warnings":[]}""",
					MediaType.APPLICATION_JSON,
				),
			)

		HttpMapsAdapter(builder, propsWithoutBuilderBaseUrl())
			.calculateRoute(listOf("ChIJa", "ChIJb"), TransportationType.WALK)

		server.verify()
	}

	@Test
	fun `routes preview 호출에도 Authorization과 X-Request-Id를 그대로 싣는다`() {
		givenIncomingRequest(BEARER_TOKEN, INCOMING_REQUEST_ID)
		val (server, builder) = bind()
		server.expect(requestTo("$BASE_URL/internal/v1/maps/routes/preview"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
			.andExpect(header(RequestId.HEADER_NAME, INCOMING_REQUEST_ID))
			.andRespond(
				withSuccess(
					"""{"encodedPolyline":null,"encodedPolylines":[],"totalDistanceMeters":0,"totalDurationSeconds":0,"legs":[],"warnings":[]}""",
					MediaType.APPLICATION_JSON,
				),
			)

		HttpMapsAdapter(builder, propsWithoutBuilderBaseUrl()).calculatePreviewRoute(
			listOf(
				RouteCalculationWaypoint("ChIJa", 37.5, 127.0),
				RouteCalculationWaypoint("ChIJb", 37.6, 127.1),
			),
		)

		server.verify()
	}

	@Test
	fun `place location 호출에도 동일한 헤더가 실린다`() {
		givenIncomingRequest(BEARER_TOKEN, INCOMING_REQUEST_ID)
		val (server, builder) = bind()
		server.expect(requestTo("$BASE_URL/internal/v1/maps/places/ChIJx/location"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
			.andExpect(header(RequestId.HEADER_NAME, INCOMING_REQUEST_ID))
			.andRespond(withSuccess("""{"latitude":"37.5","longitude":"127.0"}""", MediaType.APPLICATION_JSON))

		HttpMapsPlaceLocationAdapter(builder, propsWithoutBuilderBaseUrl()).findLocation("ChIJx")

		server.verify()
	}

	@Test
	fun `이어받을 값이 없으면 빈 헤더를 만들지 않는다`() {
		givenIncomingRequest(authorization = null, requestId = null)
		val (server, builder) = bind()
		server.expect(requestTo("$BASE_URL/internal/v1/maps/places/ChIJx/location"))
			.andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
			.andExpect(headerDoesNotExist(RequestId.HEADER_NAME))
			.andRespond(withSuccess("""{"latitude":"37.5","longitude":"127.0"}""", MediaType.APPLICATION_JSON))

		HttpMapsPlaceLocationAdapter(builder, propsWithoutBuilderBaseUrl()).findLocation("ChIJx")

		server.verify()
	}

	// MockRestServiceServer가 builder에 바인딩돼 baseUrl은 builder 쪽에서 온다. properties의 baseUrl은
	// 어댑터 생성자 시그니처를 채우기 위한 값일 뿐이라 같은 값을 넣어 둔다.
	private fun propsWithoutBuilderBaseUrl() = MapsClientProperties(java.net.URI.create(BASE_URL))

	private companion object {
		const val BASE_URL = "http://maps-service:8080"
		const val BEARER_TOKEN = "Bearer test-access-token"
		const val INCOMING_REQUEST_ID = "11111111-2222-3333-4444-555555555555"
	}
}
