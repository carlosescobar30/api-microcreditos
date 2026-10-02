package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class UsuryRateConflictException extends ApiExceptionBase {

    public UsuryRateConflictException() {
        super(ErrorCode.USURY_RATE_ALREADY_EXISTS);
    }
}
