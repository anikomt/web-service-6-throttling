package ru.ifmo.web.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationExceptionMapper  implements ExceptionMapper<AuthenticationException> {

    public Response toResponse(AuthenticationException e) {
        return Response.
                status(Response.Status.UNAUTHORIZED).
                type("text/plain").
                entity(e.getMessage()).
                build();
    }

}
