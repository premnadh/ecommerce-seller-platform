package com.premnadh.sellerplatform.orders.application.payment;

public record PaymentResult(
    boolean successful,
    String providerReference,
    String failureReason
) {

    public PaymentResult {
        if (successful) {
            if (providerReference == null
                || providerReference.isBlank()
                || providerReference.length() > 100) {
                throw new IllegalArgumentException(
                    "A successful payment requires a valid provider reference"
                );
            }

            if (failureReason != null) {
                throw new IllegalArgumentException(
                    "A successful payment cannot contain a failure reason"
                );
            }
        } else {
            if (failureReason == null
                || failureReason.isBlank()
                || failureReason.length() > 255) {
                throw new IllegalArgumentException(
                    "A failed payment requires a valid failure reason"
                );
            }

            if (providerReference != null) {
                throw new IllegalArgumentException(
                    "A failed payment cannot contain a provider reference"
                );
            }
        }
    }

    public static PaymentResult succeeded(String providerReference) {
        return new PaymentResult(true, providerReference, null);
    }

    public static PaymentResult failed(String failureReason) {
        return new PaymentResult(false, null, failureReason);
    }
}
