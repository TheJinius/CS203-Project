package com.ubs.tariffapp.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class SwaggerOAuth2Controller {

    /**
     * Custom redirect handler for OAuth2 implicit flow.
     * This endpoint handles redirects from AWS Cognito after authentication.
     * For implicit flow, tokens are handled entirely on the client side via JavaScript.
     */
    @GetMapping("/oauth2-redirect.html")
    public String customOAuth2Redirect(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            Model model) {
        
        // Add parameters to the model so they can be accessed in the HTML
        model.addAttribute("code", code);
        model.addAttribute("state", state);
        model.addAttribute("error", error);
        model.addAttribute("errorDescription", errorDescription);
        
        // Return the name of our custom HTML file (without .html extension)
        return "oauth2-redirect";
    }
}