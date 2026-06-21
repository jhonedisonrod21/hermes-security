package co.com.hermes.calendar.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Clientes HTTP del Auth Server hacia identity y tenant. Llevan <b>timeouts</b> de
 * conexión/lectura: el login y la emisión de token llaman a esos servicios de forma síncrona,
 * y sin timeout un servicio lento bloquearía los hilos del Auth Server (agotamiento de pool).
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder(
            @Value("${hermes.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${hermes.http.read-timeout:3s}") Duration readTimeout
    ) {
        return RestClient.builder().requestFactory(requestFactory(connectTimeout, readTimeout));
    }

    @Bean("hermesLoadBalancedRestClientBuilder")
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            @Value("${hermes.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${hermes.http.read-timeout:3s}") Duration readTimeout
    ) {
        return RestClient.builder().requestFactory(requestFactory(connectTimeout, readTimeout));
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
