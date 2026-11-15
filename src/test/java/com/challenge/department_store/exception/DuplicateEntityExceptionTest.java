package com.challenge.department_store.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DuplicateEntityExceptionTest {

    @Test
    void testRetainsMessage() {
        DuplicateEntityException exception =
                new DuplicateEntityException("Inventory already exists");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Inventory already exists");
    }
}
