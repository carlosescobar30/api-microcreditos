package com.carlosescobar30.apimicrocreditos.common.exception.bad_request;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

import java.math.BigDecimal;

public class RateAboveUsuryException extends ApiExceptionBase {

    public RateAboveUsuryException(BigDecimal usuryRateEa) {
        super(ErrorCode.RATE_ABOVE_USURY,
                "The interest and penalty rates cannot exceed the usury rate in force (" + usuryRateEa + " E.A.)");
    }
}
