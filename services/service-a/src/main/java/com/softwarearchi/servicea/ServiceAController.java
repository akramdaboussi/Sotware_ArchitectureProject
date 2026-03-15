package com.softwarearchi.servicea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Service A – protected by Nginx auth_request.
 * All requests here already have a validated JWT (checked by Nginx → Auth
 * /validate).
 */
@RestController
@RequestMapping("/a")
@CrossOrigin(origins = "*")
public class ServiceAController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAController.class);

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        logger.info("[SERVICE-A] GET /a/hello — request received");
        return ResponseEntity.ok(Map.of(
                "service", "A",
                "message", "Hello from Service A! Your JWT was validated by Nginx + Auth Service.",
                "status", "OK"));
    }

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> data() {
        logger.info("[SERVICE-A] GET /a/data — returning sample data");
        return ResponseEntity.ok(Map.of(
                "service", "A",
                "items", List.of(
                        Map.of("id", 1, "name", "Product Alpha", "price", 19.99),
                        Map.of("id", 2, "name", "Product Beta", "price", 34.50),
                        Map.of("id", 3, "name", "Product Gamma", "price", 8.00))));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("service", "A", "status", "UP"));
    }
}
