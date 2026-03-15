package com.softwarearchi.servicea.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarearchi.servicea.models.Project;
import com.softwarearchi.servicea.repositories.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProjectRepository projectRepository;

    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            // Since Nginx already validated the signature via Auth Service, 
            // we can just decode the payload directly.
            String[] splitToken = token.split("\\.");
            if (splitToken.length < 2) return null;
            
            String payload = new String(Base64.getUrlDecoder().decode(splitToken[1]));
            JsonNode claims = objectMapper.readTree(payload);
            
            if (claims.has("userId")) {
                return claims.get("userId").asLong();
            }
        } catch (Exception e) {
            logger.error("Failed to parse JWT payload", e);
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<Project>> getMyProjects(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<Project> projects = projectRepository.findByOwnerId(userId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectDetails(
            @PathVariable Long id, 
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Project project = projectOpt.get();
        if (!project.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<Project> createProject(
            @RequestBody Project project, 
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        project.setOwnerId(userId);
        Project savedProject = projectRepository.save(project);
        logger.info("Created new project: {} for user {}", savedProject.getId(), userId);
        return ResponseEntity.status(201).body(savedProject);
    }
}
