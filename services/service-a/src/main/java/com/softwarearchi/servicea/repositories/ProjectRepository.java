package com.softwarearchi.servicea.repositories;

import com.softwarearchi.servicea.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);
}
