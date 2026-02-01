package com.unite.repository;

import com.unite.model.WorkflowExecutionEntity;
import com.unite.model.WorkflowExecutionEntity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecutionEntity, String> {

    List<WorkflowExecutionEntity> findByWorkflowDefinitionId(String workflowDefinitionId);

    List<WorkflowExecutionEntity> findByStatus(ExecutionStatus status);

    Optional<WorkflowExecutionEntity> findByCaseId(String caseId);

    List<WorkflowExecutionEntity> findByWorkflowDefinitionIdAndStatus(String workflowDefinitionId, ExecutionStatus status);
}
