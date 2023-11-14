package org.folio.list.controller;

import lombok.extern.slf4j.Slf4j;
import org.folio.list.domain.dto.ListAppError;
import org.folio.list.domain.dto.Parameter;
import org.folio.list.exception.AbstractListException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

@ControllerAdvice
@Slf4j
public class ListExceptionHandler {
  private static final String INVALID_REQUEST_ERROR_CODE = "invalid.request";
  private static final String INVALID_REQUEST_MESSAGE = "Request failed. URL: {}. Failure reason : {}";

  @ExceptionHandler(AbstractListException.class)
  public ResponseEntity<ListAppError> exceptionHandlerForList(AbstractListException exception,
                                                              ServletWebRequest webRequest) {
    String url = webRequest.getHttpMethod() + " " + webRequest.getRequest().getRequestURI();
    log.error(INVALID_REQUEST_MESSAGE, url, exception.getMessage());
    return new ResponseEntity<>(exception.getError(), exception.getHttpStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ListAppError> handleValidationExceptions(MethodArgumentNotValidException exception,
                                                                 ServletWebRequest webRequest) {
    String url = webRequest.getHttpMethod() + " " + webRequest.getRequest().getRequestURI();
    log.error(INVALID_REQUEST_MESSAGE, url, exception.getMessage());
    List<Parameter> errorParams = exception.getBindingResult()
      .getAllErrors().stream()
      .filter(FieldError.class::isInstance)
      .map(
        error -> new Parameter()
          .key(((FieldError) error).getField())
          .value(error.getDefaultMessage())
      )
      .toList();
    ListAppError errors = new ListAppError()
      .code(INVALID_REQUEST_ERROR_CODE)
      .parameters(errorParams);
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
  public ResponseEntity<ListAppError> handleValidationExceptions2(Exception exception,
                                                                  ServletWebRequest webRequest) {
    String url = webRequest.getHttpMethod() + " " + webRequest.getRequest().getRequestURI();
    log.error(INVALID_REQUEST_MESSAGE, url, exception.getMessage());
    ListAppError errors = new ListAppError()
      .code(INVALID_REQUEST_ERROR_CODE)
      .addParametersItem(new Parameter().key("error.reason").value(exception.getMessage()));
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }
}
