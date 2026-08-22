package com.carlosescobar30.apimicrocreditos.common.exception.unauthorized;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class RefreshTokenInvalidException extends ApiExceptionBase {
    public RefreshTokenInvalidException() {
        super(ErrorCode.REFRESH_TOKEN_INVALID);
    }
}
