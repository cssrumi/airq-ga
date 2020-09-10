package pl.airq.ga.infrastructure.rest;

import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RequestInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInterceptor.class);

    @Inject
    RequestTimer requestTimer;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        final String method = context.getMethod();
        final String path = Optional.ofNullable(context.getUriInfo()).map(UriInfo::getPath).orElse("unknown path");
        LOGGER.info("Request {} {} started at: {}", method, path, requestTimer.startedAt());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LOGGER.info("Request processed after {} milliseconds", requestTimer.processTimeInMillis());
    }
}
