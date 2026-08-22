package com.carlosescobar30.apimicrocreditos.common.exception;

import com.carlosescobar30.apimicrocreditos.common.factory.ProblemDetailFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(ApiExceptionBase.class)
    public ProblemDetail handlerApiException (ApiExceptionBase exception){


        ErrorCode error = exception.getErrorCode();
        ProblemDetail problem = ProblemDetailFactory.build(error.getStatus(),error.getCode(),exception.getMessage());

        if(error.getStatus().is4xxClientError()) {
            log.warn("Client Error: [{}] : {}. ProblemID: {}",
                    error.getCode(),
                    exception.getMessage(),
                    problem.getProperties().get("traceId"));
        }
        else {
            log.error("Business Error: [{}] : {}, ProblemID: {}",
                    error.getCode(),
                    exception.getMessage(),
                    problem.getProperties().get("traceId"),
                    exception);
        }

        return problem;

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handlerValidationException (MethodArgumentNotValidException exception){

        ErrorCode error = ErrorCode.VALIDATION_ERROR;
        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );


        Map<String, String> fields = exception.getBindingResult().getFieldErrors()
                .stream().collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Invalid" : fe.getDefaultMessage(),
                        (a,b) -> a
                ));

         problem.setProperty("fields", fields);

         log.error("Validation Method Argument Error [{}] : {}. ProblemID: {}",
                 error.getCode(),
                 exception.getMessage(),
                 problem.getProperties().get("traceId"),
                 exception);
         return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handlerAuthenticationException (AuthenticationException exception){

        ErrorCode error = ErrorCode.UNAUTHENTICATED;
        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );

        log.warn("Authentification Error: [{}] : {}. ProblemID: {}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                problem.getProperties().get("traceId"));

        return problem;

    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handlerEntityNotFoundException (EntityNotFoundException exception) {

        ErrorCode error = ErrorCode.RESOURCE_NOT_FOUND;
        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );

        log.error("Entity Not Found Error [{}] : {}. ProblemID: {}",
                error.getCode(),
                exception.getMessage(),
                problem.getProperties().get("traceId"),
                exception);

        return problem;

    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handlerDataIntegrityViolationException (DataIntegrityViolationException exception){

        ErrorCode error = ErrorCode.DATA_CONFLICT;

        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );

        log.error("Conflict in Insertion Error [{}] : {}. ProblemID: {}",
                error.getCode(),
                exception.getMessage(),
                problem.getProperties().get("traceId"),
                exception);

        return  problem;

    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handlerAccessDeniedException(AccessDeniedException exception) throws AccessDeniedException {

        throw exception;

    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handlerException (Exception exception) {

        ErrorCode error = ErrorCode.INTERNAL_ERROR_SERVER;

        ProblemDetail problem = ProblemDetailFactory.build(
                error.getStatus(),
                error.getCode(),
                error.getMessage()
        );
        log.error("Unexpected Problem ID: [{}] {} ",
                problem.getProperties().get("traceId"),
                error.getMessage(),
                exception);
        return problem;
    }


}
