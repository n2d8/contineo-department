package com.challenge.department_store.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.jwt.enabled=false")
@AutoConfigureMockMvc
class JwtAuthenticationDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsApiRequestsWithoutCredentialsWhenAuthenticationIsDisabled() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }
}
