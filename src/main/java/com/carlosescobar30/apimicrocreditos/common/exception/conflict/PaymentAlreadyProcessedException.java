package com.carlosescobar30.apimicrocreditos.common.exception.conflict;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class PaymentAlreadyProcessedException extends ApiExceptionBase {

    public PaymentAlreadyProcessedException(String currentStatus) {
        super(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                "The payment is already " + currentStatus + " and its status cannot change");
    }
}
