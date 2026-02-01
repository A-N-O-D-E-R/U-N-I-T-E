package com.unite.repository;

import com.unite.model.WorkflowDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, String> {

    Optional<WorkflowDefinitionEntity> findByNameAndVersion(String name, String version);

    List<WorkflowDefinitionEntity> findByActive(Boolean active);

    List<WorkflowDefinitionEntity> findByNameContainingIgnoreCase(String name);

    boolean existsByNameAndVersion(String name, String version);
}
