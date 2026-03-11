package com.salias.storems.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storeManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Store Management System API")
                        .description("REST API for stores, employees, products, sales, and manager assignments.")
                        .version("v1")
                        .contact(new Contact().name("StoreMS Team"))
                        .license(new License().name("Internal Use")));
    }
}
