package cc.mrbird.febs.gateway.common.filter;

import cc.mrbird.febs.common.entity.constant.FebsConstant;
import cc.mrbird.febs.gateway.common.properties.FebsGatewayProperties;
import cc.mrbird.febs.gateway.enhance.service.RouteEnhanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Base64Utils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author MrBird
 */
@Slf4j
@Component
@Order(0)
public class FebsGatewayRequestFilter implements GlobalFilter {

    @Autowired
    private FebsGatewayProperties properties;
    @Autowired
    private RouteEnhanceService routeEnhanceService;

    @Value("${febs.gateway.enhance:false}")
    private Boolean routeEhance;

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (routeEhance) {
            Mono<Void> balckListResult = routeEnhanceService.filterBalckList(exchange);
            if (balckListResult != null) {
                routeEnhanceService.saveBlockLogs(exchange);
                return balckListResult;
            }
            Mono<Void> rateLimitResult = routeEnhanceService.filterRateLimit(exchange);
            if (rateLimitResult != null) {
                routeEnhanceService.saveRateLimitLogs(exchange);
                return rateLimitResult;
            }
            routeEnhanceService.saveRequestLogs(exchange);
        }

        byte[] token = Base64Utils.encode((FebsConstant.GATEWAY_TOKEN_VALUE).getBytes());
        String[] headerValues = {new String(token)};
        ServerHttpRequest build = exchange.getRequest().mutate().header(FebsConstant.GATEWAY_TOKEN_HEADER, headerValues).build();
        ServerWebExchange newExchange = exchange.mutate().request(build).build();
        return chain.filter(newExchange);
    }
}
