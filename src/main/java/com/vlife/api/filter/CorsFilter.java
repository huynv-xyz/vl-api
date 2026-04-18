package com.vlife.api.filter;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.*;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.*;
import org.reactivestreams.Publisher;

import java.util.Set;

@Filter("/**")
public class CorsFilter implements HttpServerFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://14.225.255.170",
            "https://vlife.com.vn",
            "https://cms.vlife.com.vn",
            "http://cms.vlife.com.vn"
    );

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                                      ServerFilterChain chain) {

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return Publishers.just(applyCors(request, HttpResponse.ok()));
        }

        return Publishers.map(chain.proceed(request), res -> applyCors(request, res));
    }

    private MutableHttpResponse<?> applyCors(HttpRequest<?> request,
                                             MutableHttpResponse<?> response) {

        String origin = request.getHeaders().getOrigin().orElse("");

        if (ALLOWED_ORIGINS.contains(origin)) {
            response.header("Access-Control-Allow-Origin", origin);
        }

        return response
                .header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Allow-Credentials", "true");
    }
}