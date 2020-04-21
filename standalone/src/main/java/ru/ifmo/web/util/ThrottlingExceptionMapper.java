package ru.ifmo.web.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ThrottlingExceptionMapper implements ExceptionMapper<ThrottlingException> {

    @Override
    public Response toResponse(ThrottlingException e) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Слишком много запросов. Сервис занят.")
                .build();
    }
}