package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.global.exception.MapsServiceUnavailableException
import com.ktcloud.travelplanner.place.port.PlaceLocation
import com.ktcloud.travelplanner.place.port.PlaceLocationPort
import com.ktcloud.travelplanner.route.model.TransportationType
import com.ktcloud.travelplanner.route.port.RouteCalculation
import com.ktcloud.travelplanner.route.port.RouteCalculationPort
import com.ktcloud.travelplanner.route.port.RouteLegCalculation
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.net.URI

@ConfigurationProperties("app.external.maps")
data class MapsClientProperties(val baseUrl: URI)

// Travel → Maps 어댑터. 장애 처리는 포트별로 다르다(SERVICE_COMMUNICATION_BOUNDARIES.md 4절):
//   - 경로 계산(RouteCalculationPort): Maps 장애 시 503으로 실패(fail-closed) —
//     "모름"을 "경로 없음"으로 뭉개면 화면이 잘못된 확정 응답을 준다.
//   - 장소 좌표 조회(PlaceLocationPort): 실패 시 null 완화 — 좌표 못 구한 핀만 빠지고
//     나머지 지도는 200으로 정상 응답한다.
@Component
class HttpMapsAdapter(
	restClientBuilder: RestClient.Builder,
	properties: MapsClientProperties,
) : RouteCalculationPort {
	private val client = restClientBuilder.baseUrl(properties.baseUrl.toASCIIString()).build()

	override fun calculateRoute(googlePlaceIds: List<String>, transportationType: TransportationType): RouteCalculation {
		val response = try {
			client.post().uri("/internal/v1/maps/routes/calculate")
				.headers { it.applyIncomingRequestContext() }
				.body(HttpRouteCalculationRequest(googlePlaceIds, transportationType))
				.retrieve().body(HttpRouteCalculationResponse::class.java)
		} catch (exception: RestClientException) {
			// 연결 실패·timeout·4xx·5xx 전부 "경로 계산 불가"로 묶어 503으로 올린다.
			throw MapsServiceUnavailableException(cause = exception)
		} ?: throw MapsServiceUnavailableException()
		return RouteCalculation(response.encodedPolyline, response.encodedPolylines, response.totalDistanceMeters, response.totalDurationSeconds,
			response.legs.map { RouteLegCalculation(it.distanceMeters, it.durationSeconds) }, response.warnings)
	}

	override fun calculatePreviewRoute(waypoints: List<com.ktcloud.travelplanner.route.port.RouteCalculationWaypoint>): RouteCalculation =
		throw UnsupportedOperationException("Maps preview HTTP contract is not available yet.")

}

@Component
class HttpMapsPlaceLocationAdapter(
	restClientBuilder: RestClient.Builder,
	properties: MapsClientProperties,
) : PlaceLocationPort {
	private val client = restClientBuilder.baseUrl(properties.baseUrl.toASCIIString()).build()

	override fun findLocation(googlePlaceId: String): PlaceLocation? = try {
		client.get().uri("/internal/v1/maps/places/{id}/location", googlePlaceId)
			.headers { it.applyIncomingRequestContext() }
			.retrieve()
			.body(HttpPlaceLocationResponse::class.java)?.let { PlaceLocation(BigDecimal(it.latitude), BigDecimal(it.longitude)) }
	} catch (exception: RestClientException) {
		// 404(좌표 없음)든 5xx/timeout(Maps 장애)든 이 핀은 좌표 없이 두고 나머지는 정상 응답한다.
		null
	}
}

data class HttpRouteCalculationRequest(val googlePlaceIds: List<String>, val transportationType: TransportationType)
data class HttpRouteCalculationResponse(val encodedPolyline: String?, val encodedPolylines: List<String>, val totalDistanceMeters: Long, val totalDurationSeconds: Long, val legs: List<HttpRouteLegResponse>, val warnings: List<String>)
data class HttpRouteLegResponse(val distanceMeters: Long, val durationSeconds: Long)
data class HttpPlaceLocationResponse(val latitude: String, val longitude: String)
