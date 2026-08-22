package com.carlosescobar30.apimicrocreditos.common.factory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

public class ProblemDetailFactory {

    public static ProblemDetail build(HttpStatus status, String code, String details) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, details);
        problem.setProperty("code", code);
        problem.setProperty("traceId", UUID.randomUUID().toString());
        return problem;

    }

}
