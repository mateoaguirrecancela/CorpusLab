package es.udc.fic.corpuslab.common.exceptions;

import org.springframework.http.HttpStatus;

public interface TranslatableApiException {

    String getMessageKey();

    Object[] getMessageArgs();

    HttpStatus getHttpStatus();
}
