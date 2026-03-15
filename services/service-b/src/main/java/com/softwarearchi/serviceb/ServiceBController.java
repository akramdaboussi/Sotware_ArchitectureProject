package com.softwarearchi.serviceb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Service B – protected by Nginx auth_request.
 * All requests here already have a validated JWT (checked by Nginx → Auth
 * /validate).
 */
@RestController
@RequestMapping("/b")
@CrossOrigin(origins = "*")
public class ServiceBController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBController.class);

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        logger.info("[SERVICE-B] GET /b/hello — request received");
        return ResponseEntity.ok(Map.of(
                "service", "B",
                "message", "Hello from Service B! Your JWT was validated by Nginx + Auth Service.",
                "status", "OK"));
    }

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> data() {
        logger.info("[SERVICE-B] GET /b/data — returning sample analytics data");
        return ResponseEntity.ok(Map.of(
                "service", "B",
                "analytics", List.of(
                        Map.of("metric", "users_today", "value", 142),
                        Map.of("metric", "registrations", "value", 37),
                        Map.of("metric", "verified_emails", "value", 29))));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("service", "B", "status", "UP"));
    }
}
