package com.banking.system.exceptions;

import com.banking.system.enums.BankingProcessorError;

import static com.banking.system.Constants.COLON;

public class BaseRuntimeException extends RuntimeException {

    public BaseRuntimeException(BankingProcessorError error) {
        super(getMessage(error));
    }

//    public BaseRuntimeException(List<BankingProcessorError> errors) {
//        super(getMessage(errors));
//    }

    private static String getMessage(BankingProcessorError error) {
        return String.join(COLON, error.getErrorCode(), error.getOptionalDebugMessage());
    }

//    private static String getMessage(List<BankingProcessorError> errors) {
//        return errors.stream()
//                .map(BaseRuntimeException::getMessage)
//                .collect(Collectors.joining(System.lineSeparator()));
//    }
}
