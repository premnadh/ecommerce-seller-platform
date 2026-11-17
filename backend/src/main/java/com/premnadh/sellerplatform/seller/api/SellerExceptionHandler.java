package com.premnadh.sellerplatform.seller.api;

import com.premnadh.sellerplatform.seller.exception.DuplicateSellerEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SellerExceptionHandler {

    @ExceptionHandler(DuplicateSellerEmailException.class)
    public ProblemDetail handleDuplicateEmail(
            DuplicateSellerEmailException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }
}