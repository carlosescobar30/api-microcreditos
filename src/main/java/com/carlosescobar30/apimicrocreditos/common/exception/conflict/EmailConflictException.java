package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class EmailConflictException extends ApiExceptionBase {

    public EmailConflictException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
