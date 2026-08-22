package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.common.factory.ProblemDetailFactory;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


import java.io.IOException;

import static com.carlosescobar30.apimicrocreditos.iam.security.SecurityAttributes.AUTH_ERROR_ATTRIBUTE;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        ErrorCode error = (ErrorCode) request.getAttribute(AUTH_ERROR_ATTRIBUTE);

        if (error == null){
            error = ErrorCode.UNAUTHENTICATED;
        }

        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );

        response.setStatus(error.getStatus().value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getOutputStream(), problem);

    }
}
