package com.smartfinancialexpenseanalysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthorized requests (e.g. USER accessing an ADMIN endpoint)
 * by returning an HTTP 403 JSON error.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse apiResponse = new ApiResponse(false, "Access denied: You do not have permission to access this resource");
        response.getOutputStream().println(objectMapper.writeValueAsString(apiResponse));
    }
}
