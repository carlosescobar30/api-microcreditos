package com.carlosescobar30.apimicrocreditos.common.exception.bad_request;

import com.carlosescobar30.apimicrocreditos.common.exception.ApiExceptionBase;
import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;

public class ActionNotPermitted extends ApiExceptionBase {
    public ActionNotPermitted() {
        super(ErrorCode.ACTION_NOT_PERMITTED);
    }

    public ActionNotPermitted(String message) {
        super(ErrorCode.ACTION_NOT_PERMITTED, message);
    }
}
