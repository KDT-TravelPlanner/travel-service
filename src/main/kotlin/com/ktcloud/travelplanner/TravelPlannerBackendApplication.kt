package com.ktcloud.travelplanner

import com.ktcloud.travelplanner.global.security.JwtProperties
import com.ktcloud.travelplanner.maps.adapter.MapsClientProperties
import com.ktcloud.travelplanner.membership.adapter.IdentityClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
	JwtProperties::class,
	MapsClientProperties::class,
	IdentityClientProperties::class,
)
class TravelPlannerBackendApplication

fun main(args: Array<String>) {
	runApplication<TravelPlannerBackendApplication>(*args)
}
