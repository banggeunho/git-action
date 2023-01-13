package com.sweep.jaksim31.exception.handler;
import com.sweep.jaksim31.exception.BadRequestException;
import com.sweep.jaksim31.exception.BizException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * packageName :  com.sweep.jaksim31.exception.handler
 * fileName : ApiExceptionHandler
 * author :  방근호
 * date : 2023-01-13
 * description : api 예외처리 핸들러
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2023-01-13           방근호             최초 생성
 *
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BizException ex) {
        System.out.println("Error Message : " + ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage()),
                ex.getBaseExceptionType().getHttpStatus()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {

        return new ResponseEntity<>(
                new ErrorResponse("system error"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}