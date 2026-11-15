package com.challenge.department_store.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotFoundExceptionTest {

    @Test
    void testRetainsMessage() {
        NotFoundException exception = new NotFoundException("Inventory not found");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Inventory not found");
    }
}
