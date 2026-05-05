package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.WorkspaceAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceAuditEventRepository extends JpaRepository<WorkspaceAuditEvent, Long> {
    List<WorkspaceAuditEvent> findAllByOrderByCreatedAtDesc();
}
