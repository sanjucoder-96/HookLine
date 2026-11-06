package com.hookline.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(HooklineProperties.class)
public class RestClientConfig {

    /**
     * The HTTP client used to deliver webhooks. SHORT, explicit timeouts are the
     * point: a receiver that accepts the connection then hangs must fail fast into
     * our retry path, not pin a delivery worker for 30 seconds.
     */
    @Bean
    RestClient deliveryRestClient(HooklineProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getDelivery().getHttp().getConnectTimeoutMs());
        factory.setReadTimeout(props.getDelivery().getHttp().getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }
}
