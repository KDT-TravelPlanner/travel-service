package com.ktcloud.travelplanner.membership.adapter

import com.ktcloud.travelplanner.global.external.externalHttpRequestFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Duration

@ConfigurationProperties("app.external.identity")
data class IdentityClientProperties(
	val baseUrl: URI,
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(3),
)

// HttpMembershipUserAdapter 전용 RestClient. 타임아웃 없는 클라이언트를 만들지 않기 위해
// 반드시 externalHttpRequestFactory로 request factory를 준다(HttpMapsAdapter와 동일 규칙).
@Configuration(proxyBeanMethods = false)
class IdentityClientConfig {
	@Bean
	fun identityRestClient(
		restClientBuilder: RestClient.Builder,
		properties: IdentityClientProperties,
	): RestClient = restClientBuilder.clone()
		.baseUrl(properties.baseUrl.toASCIIString().trimEnd('/'))
		.requestFactory(externalHttpRequestFactory(properties.connectTimeout, properties.readTimeout))
		.build()
}
