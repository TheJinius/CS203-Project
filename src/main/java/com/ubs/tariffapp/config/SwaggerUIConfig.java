package com.ubs.tariffapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class SwaggerUIConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            // Add any additional OpenAPI customizations here
            openApi.info(openApi.getInfo()
                .description(openApi.getInfo().getDescription() + 
                    "\n\n**OAuth2 Authentication:** When using the 'Authorize' button, " +
                    "if a new tab opens, please allow popups for this site or copy the " +
                    "authorization code from the redirect URL back to the original Swagger UI tab."));
        };
    }
}