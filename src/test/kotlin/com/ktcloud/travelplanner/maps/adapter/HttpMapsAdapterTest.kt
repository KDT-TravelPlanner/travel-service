package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.global.exception.MapsServiceUnavailableException
import com.ktcloud.travelplanner.route.model.TransportationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.io.IOException
import java.math.BigDecimal
import java.net.URI

// Maps 장애를 포트별 계약대로 옮기는지 고정한다:
//   calculateRoute -> MapsServiceUnavailableException(503)
//   findLocation   -> null 완화
class HttpMapsAdapterTest {
	private fun bind(): Pair<MockRestServiceServer, RestClient.Builder> {
		val builder = RestClient.builder().baseUrl(BASE_URL)
		return MockRestServiceServer.bindTo(builder).build() to builder
	}

	private fun props() = MapsClientProperties(URI.create(BASE_URL))

	@Test
	fun `calculateRoute - maps 5xx면 MapsServiceUnavailableException`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/routes/calculate"))
			.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

		assertThrows<MapsServiceUnavailableException> {
			HttpMapsAdapter(builder, props()).calculateRoute(listOf("A", "B"), TransportationType.WALK)
		}
		server.verify()
	}

	@Test
	fun `calculateRoute - 연결 실패도 MapsServiceUnavailableException`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/routes/calculate"))
			.andRespond(withException(IOException("connection refused")))

		assertThrows<MapsServiceUnavailableException> {
			HttpMapsAdapter(builder, props()).calculateRoute(listOf("A", "B"), TransportationType.WALK)
		}
		server.verify()
	}

	@Test
	fun `calculateRoute - 정상 응답은 그대로 매핑`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/routes/calculate"))
			.andRespond(
				withSuccess(
					"""{"encodedPolyline":"abc","encodedPolylines":["abc"],"totalDistanceMeters":10,"totalDurationSeconds":20,"legs":[{"distanceMeters":10,"durationSeconds":20}],"warnings":[]}""",
					MediaType.APPLICATION_JSON,
				),
			)

		val result = HttpMapsAdapter(builder, props()).calculateRoute(listOf("A", "B"), TransportationType.WALK)

		assertEquals(10, result.totalDistanceMeters)
		assertEquals(1, result.legs.size)
		server.verify()
	}

	@Test
	fun `findLocation - maps 5xx면 null로 완화`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/places/ChIJx/location"))
			.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

		assertNull(HttpMapsPlaceLocationAdapter(builder, props()).findLocation("ChIJx"))
		server.verify()
	}

	@Test
	fun `findLocation - 404도 null`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/places/ChIJx/location"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND))

		assertNull(HttpMapsPlaceLocationAdapter(builder, props()).findLocation("ChIJx"))
		server.verify()
	}

	@Test
	fun `findLocation - 정상 응답은 좌표 반환`() {
		val (server, builder) = bind()
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("$BASE_URL/internal/v1/maps/places/ChIJx/location"))
			.andRespond(withSuccess("""{"latitude":"37.5","longitude":"127.0"}""", MediaType.APPLICATION_JSON))

		val location = HttpMapsPlaceLocationAdapter(builder, props()).findLocation("ChIJx")

		assertEquals(BigDecimal("37.5"), location?.latitude)
		assertEquals(BigDecimal("127.0"), location?.longitude)
		server.verify()
	}

	private companion object {
		const val BASE_URL = "http://maps-service:8080"
	}
}
