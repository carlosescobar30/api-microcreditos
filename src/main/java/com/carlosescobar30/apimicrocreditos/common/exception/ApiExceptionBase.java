package com.carlosescobar30.apimicrocreditos.common.exception;

public abstract class ApiExceptionBase extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiExceptionBase(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiExceptionBase(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode(){
        return errorCode;
    }
}
