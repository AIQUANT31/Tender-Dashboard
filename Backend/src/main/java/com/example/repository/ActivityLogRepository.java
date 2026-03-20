package com.example.repository;

import com.example.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    List<ActivityLog> findByUsernameOrderByTimestampDesc(String username);
    
    Page<ActivityLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
    
    List<ActivityLog> findByEntityIdOrderByTimestampDesc(Long entityId);
    
    List<ActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    
    List<ActivityLog> findByActionContainingIgnoreCaseOrderByTimestampDesc(String action);
    
    @Query("SELECT a FROM ActivityLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<ActivityLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM ActivityLog a WHERE a.username = :username AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<ActivityLog> findByUsernameAndDateRange(@Param("username") String username,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.username = :username")
    long countByUsername(@Param("username") String username);
}
