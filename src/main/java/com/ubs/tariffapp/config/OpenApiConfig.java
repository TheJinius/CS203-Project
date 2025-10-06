package com.ubs.tariffapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${aws.cognito.domain}")
    private String cognitoDomain;

    @Value("${aws.cognito.region}")
    private String awsRegion;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Bean
    public OpenAPI customOpenAPI() {
        // OAuth2 Implicit Flow for Cognito
        String authorizationUrl = String.format("https://%s.auth.%s.amazoncognito.com/oauth2/authorize",
            cognitoDomain, awsRegion);

        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("CS203 Tariff Application API")
                .version("1.0")
                .description("Comprehensive API documentation for the tariff application system. Uses OAuth2 authentication via AWS Cognito."))
            .addSecurityItem(new SecurityRequirement()
                .addList("oauth2"))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("oauth2",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)
                        .description("OAuth2 authentication via AWS Cognito (Implicit Flow).")
                        .flows(new OAuthFlows()
                            .implicit(new OAuthFlow()
                                .authorizationUrl(authorizationUrl)
                                .scopes(new Scopes()
                                    .addString("openid", "")
                                )
                            )
                        )
                )
                .addSecuritySchemes("bearerAuth",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Bearer token authentication. Paste your access token from OAuth2 flow here.")
                )
            );
    }
}