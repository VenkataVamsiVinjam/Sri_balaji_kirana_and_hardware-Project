package com.sribalaji.erp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles both REST (/api/**) and web (Thymeleaf) requests from a single handler per
 * exception type, and decides the response format based on the request path.
 * (Two separate @ExceptionHandler methods for the same exception type in one
 * @ControllerAdvice is illegal in Spring and would fail at startup with an
 * AmbiguousMappingException - this class deliberately avoids that.)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest req) {
        if (isApiRequest(req)) {
            return jsonError(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return webError(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        if (isApiRequest(req)) {
            return jsonError(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return webError(ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> jsonError(String message, HttpStatus status) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    private ModelAndView webError(String message) {
        ModelAndView mav = new ModelAndView("error/business-error");
        mav.addObject("message", message);
        return mav;
    }

    private boolean isApiRequest(HttpServletRequest req) {
        return req.getRequestURI() != null && req.getRequestURI().startsWith("/api/");
    }
}
