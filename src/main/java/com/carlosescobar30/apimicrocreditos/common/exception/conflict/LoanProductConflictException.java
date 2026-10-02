package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class LoanProductConflictException extends ApiExceptionBase {

    public LoanProductConflictException() {
        super(ErrorCode.LOAN_PRODUCT_ALREADY_EXISTS);
    }
}
