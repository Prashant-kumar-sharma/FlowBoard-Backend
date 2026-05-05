package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
    List<Workspace> findByVisibility(Workspace.Visibility visibility);
    boolean existsByNameAndOwnerId(String name, Long ownerId);

    @Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.userId = :userId")
    List<Workspace> findByMemberUserId(Long userId);
}
