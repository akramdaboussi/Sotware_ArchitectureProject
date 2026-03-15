package com.softwarearchi.serviceb.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarearchi.serviceb.models.Task;
import com.softwarearchi.serviceb.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TaskRepository taskRepository;

    private JsonNode extractClaimsFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String[] splitToken = token.split("\\.");
            if (splitToken.length < 2) return null;
            
            String payload = new String(Base64.getUrlDecoder().decode(splitToken[1]));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            logger.error("Failed to parse JWT payload", e);
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getTasksByProject(
            @RequestParam Long projectId, 
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        JsonNode claims = extractClaimsFromToken(authHeader);
        if (claims == null) {
            return ResponseEntity.status(401).build();
        }
        
        // In a real app we'd also check if the user has access to this project
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Task task, 
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        JsonNode claims = extractClaimsFromToken(authHeader);
        if (claims == null) {
            return ResponseEntity.status(401).build();
        }
        
        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("TODO");
        }
        
        Task savedTask = taskRepository.save(task);
        logger.info("Created new task: {} for project {}", savedTask.getId(), task.getProjectId());
        return ResponseEntity.status(201).body(savedTask);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        JsonNode claims = extractClaimsFromToken(authHeader);
        if (claims == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Task task = taskOpt.get();
        if (body.containsKey("status")) {
            task.setStatus(body.get("status"));
            Task updatedTask = taskRepository.save(task);
            return ResponseEntity.ok(updatedTask);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id, 
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        JsonNode claims = extractClaimsFromToken(authHeader);
        if (claims == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        taskRepository.delete(taskOpt.get());
        logger.info("Deleted task: {}", id);
        return ResponseEntity.ok().build();
    }
}
