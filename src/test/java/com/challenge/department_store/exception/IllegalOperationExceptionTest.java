package com.challenge.department_store.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IllegalOperationExceptionTest {

    @Test
    void testRetainsMessage() {
        IllegalOperationException exception =
                new IllegalOperationException("Quantity must not be negative");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quantity must not be negative");
    }
}
