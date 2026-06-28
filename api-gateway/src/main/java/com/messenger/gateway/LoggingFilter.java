package com.messenger.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.info("REQUEST: {} {} from {}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.info("RESPONSE: {} for {} {}",
                    exchange.getResponse().getStatusCode(),
                    request.getMethod(),
                    request.getURI());
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}