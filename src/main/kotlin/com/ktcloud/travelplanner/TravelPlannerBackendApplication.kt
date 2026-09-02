package com.ktcloud.travelplanner

import com.ktcloud.travelplanner.global.security.JwtProperties
import com.ktcloud.travelplanner.place.config.GooglePlacesProperties
import com.ktcloud.travelplanner.route.config.GoogleRoutesProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
	JwtProperties::class,
	GooglePlacesProperties::class,
	GoogleRoutesProperties::class,
)
class TravelPlannerBackendApplication

fun main(args: Array<String>) {
	runApplication<TravelPlannerBackendApplication>(*args)
}
