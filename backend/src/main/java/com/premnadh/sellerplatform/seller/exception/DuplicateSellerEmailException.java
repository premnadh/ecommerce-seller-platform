package com.premnadh.sellerplatform.seller.exception;

public class DuplicateSellerEmailException extends RuntimeException {

    public DuplicateSellerEmailException(String email) {
        super("A seller account with email " + email + " already exists");
    }
}