package com.carlosescobar30.apimicrocreditos.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    // BAD_REQUEST 400
    VALIDATION_ERROR ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "One or more fields are invalid"),
    ACTION_NOT_PERMITTED("ACTION_NOT_PERMITTED", HttpStatus.BAD_REQUEST,"The resource does not permit the requested action"),
    RATE_ABOVE_USURY("RATE_ABOVE_USURY", HttpStatus.BAD_REQUEST, "The rate exceeds the usury rate in force"),

    //UNAUTHORIZED 401
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "The refresh token sent is invalid"),
    UNAUTHENTICATED("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "The user's identity could not be authenticated"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "The access token has expired"),

    //FORBIDDEN 403
    FORBIDDEN_REQUEST("FORBIDDEN_REQUEST", HttpStatus.FORBIDDEN, "The user does not have the necessary role for this request"),

    //NOT_FOUD 404
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "This resource does not exist"),

    //CONFLICT 409
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", HttpStatus.CONFLICT, "This username is not available"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "This email is not available"),
    DATA_CONFLICT ("DATA_CONFLICT", HttpStatus.CONFLICT,"The insertion could not be completed due to data conflicts"),
    LOAN_PRODUCT_ALREADY_EXISTS("LOAN_PRODUCT_ALREADY_EXISTS", HttpStatus.CONFLICT, "A loan product with this name already exists"),
    USURY_RATE_ALREADY_EXISTS("USURY_RATE_ALREADY_EXISTS", HttpStatus.CONFLICT, "A usury rate is already registered for this modality and month"),
    PAYMENT_ALREADY_PROCESSED("PAYMENT_ALREADY_PROCESSED", HttpStatus.CONFLICT, "The payment has already been processed and its status cannot change"),

    //INTERNAL_SERVER_ERROR 500
    INTERNAL_ERROR_SERVER ("INTERNAL_ERROR_SERVER", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error on the server");


    private final String code;
    private final HttpStatus status;
    private final String message;

}
