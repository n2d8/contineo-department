package com.challenge.department_store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI departmentStoreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Department Store Inventory API")
                        .description(
                                "API for managing product inventories with categorization capability."
                        )
                        .version("1.0.0"));
    }
}