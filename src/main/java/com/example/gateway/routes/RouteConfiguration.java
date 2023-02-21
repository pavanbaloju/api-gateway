package com.example.gateway.routes;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Slf4j
@Component
public class RouteConfiguration {

    private static final String MESSAGE = "Message";

    @Bean
    public RouteLocator getRoute(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route("getRoute", predicateSpec -> predicateSpec
                        .path("/hello")
                        .filters(gatewayFilterSpec -> gatewayFilterSpec
                                .addRequestHeader(MESSAGE, "Hello World")
                                .filter(RouteConfiguration::logRequest)
                                .addResponseHeader("HEADER", "HEADER_VALUE")
                                .modifyResponseBody(String.class, String.class, RouteConfiguration::modifyResponseBody))
                        .uri("http://localhost:8081/hello"))
                .build();
    }

    @Bean
    public RouteLocator getUsers(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route("getUsers", predicateSpec -> predicateSpec
                        .path("/users")
                        .uri("https://jsonplaceholder.typicode.com/users/"))
                .route("getUserById", t -> t.path("/users/**")
                        .filters(rw -> rw.rewritePath("/users/(?<segment>.*)", "/users/${segment}"))
                        .uri("https://jsonplaceholder.typicode.com/users/"))
                .build();
    }

    private static Publisher<String> modifyResponseBody(ServerWebExchange exchange, String responseBody) {
        log.info("Response from /hello: {}", responseBody);
        log.info("Response headers: {}", exchange.getResponse().getHeaders());
        final String upperCase = responseBody.toUpperCase(Locale.ROOT);
        log.info("Response returned by gateway: {}", upperCase);
        return Mono.just(upperCase);
    }

    private static Mono<Void> logRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        final ServerHttpRequest request = exchange.getRequest();
        log.info("Request headers value: {}", request.getHeaders().get(MESSAGE));
        return chain.filter(exchange);
    }
}
