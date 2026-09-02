package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.place.port.PlaceLocation
import com.ktcloud.travelplanner.place.port.PlaceLocationPort
import com.ktcloud.travelplanner.route.model.TransportationType
import com.ktcloud.travelplanner.route.port.RouteCalculation
import com.ktcloud.travelplanner.route.port.RouteCalculationPort
import com.ktcloud.travelplanner.route.port.RouteLegCalculation
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.net.URI

@ConfigurationProperties("app.external.maps")
data class MapsClientProperties(val baseUrl: URI)

@Component
class HttpMapsAdapter(
	restClientBuilder: RestClient.Builder,
	properties: MapsClientProperties,
) : RouteCalculationPort {
	private val client = restClientBuilder.baseUrl(properties.baseUrl.toASCIIString()).build()

	override fun calculateRoute(googlePlaceIds: List<String>, transportationType: TransportationType): RouteCalculation {
		val response = client.post().uri("/internal/v1/maps/routes/calculate")
			.body(HttpRouteCalculationRequest(googlePlaceIds, transportationType))
			.retrieve().body(HttpRouteCalculationResponse::class.java)!!
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
		client.get().uri("/internal/v1/maps/places/{id}/location", googlePlaceId).retrieve()
			.body(HttpPlaceLocationResponse::class.java)?.let { PlaceLocation(BigDecimal(it.latitude), BigDecimal(it.longitude)) }
	} catch (_: org.springframework.web.client.HttpClientErrorException.NotFound) { null }
}

data class HttpRouteCalculationRequest(val googlePlaceIds: List<String>, val transportationType: TransportationType)
data class HttpRouteCalculationResponse(val encodedPolyline: String?, val encodedPolylines: List<String>, val totalDistanceMeters: Long, val totalDurationSeconds: Long, val legs: List<HttpRouteLegResponse>, val warnings: List<String>)
data class HttpRouteLegResponse(val distanceMeters: Long, val durationSeconds: Long)
data class HttpPlaceLocationResponse(val latitude: String, val longitude: String)
