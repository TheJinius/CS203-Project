package com.ubs.tariffapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "CS203 Tariff Application API",
        version = "1.0",
        description = "API documentation for the CS203 Tariff Application"
    )
)
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("CS203 Tariff Application API")
                .version("1.0")
                .description("Comprehensive API documentation for the tariff application system"));
    }
}