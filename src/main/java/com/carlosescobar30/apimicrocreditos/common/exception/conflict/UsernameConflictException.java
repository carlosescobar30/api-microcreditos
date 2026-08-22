package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class UsernameConflictException extends ApiExceptionBase {

    public UsernameConflictException() {
        super(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
