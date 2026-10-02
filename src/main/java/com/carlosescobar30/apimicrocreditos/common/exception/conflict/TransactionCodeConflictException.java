package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class TransactionCodeConflictException extends ApiExceptionBase {

    public TransactionCodeConflictException() {
        super(ErrorCode.TRANSACTION_CODE_ALREADY_EXISTS);
    }
}
