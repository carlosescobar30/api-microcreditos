package com.carlosescobar30.apimicrocreditos.common.exception.unauthorized;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class UnauthenticatedUserException extends ApiExceptionBase {
    public UnauthenticatedUserException() {
        super(ErrorCode.UNAUTHENTICATED);
    }
}
