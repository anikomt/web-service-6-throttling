package ru.ifmo.web.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import lombok.extern.slf4j.Slf4j;
import ru.ifmo.web.util.ThrottlingException;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ThrottlingFilter implements ContainerRequestFilter, ContainerResponseFilter, ResourceFilter {
    private static AtomicInteger activeRequests = new AtomicInteger(0);
    private static final int PARALLEL_REQUEST_LIMIT = 3;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        if (activeRequests.incrementAndGet() > PARALLEL_REQUEST_LIMIT) {
            throw new ThrottlingException();
        }
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        activeRequests.decrementAndGet();
        return response;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }
}
