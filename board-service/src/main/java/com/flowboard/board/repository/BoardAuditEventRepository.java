package com.flowboard.board.repository;

import com.flowboard.board.entity.BoardAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardAuditEventRepository extends JpaRepository<BoardAuditEvent, Long> {
    List<BoardAuditEvent> findAllByOrderByCreatedAtDesc();
}
