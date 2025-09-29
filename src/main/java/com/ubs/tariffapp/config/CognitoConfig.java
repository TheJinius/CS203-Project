// package com.ubs.tariffapp.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Profile;

// @Configuration
// public class CognitoConfig {

//     @Bean
//     @Profile("local")
//     public String localRedirectUri() {
//         return "http://localhost:8080/login/oauth2/code/cognito";
//     }

//     @Bean
//     @Profile("prod")
//     public String prodRedirectUri() {
//         return "https://yourdomain.com/login/oauth2/code/cognito";
//     }
// }
