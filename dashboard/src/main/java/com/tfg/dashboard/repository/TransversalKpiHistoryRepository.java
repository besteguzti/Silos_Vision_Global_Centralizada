package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.TransversalKpiHistory;

public interface TransversalKpiHistoryRepository extends JpaRepository<TransversalKpiHistory, Long> {

    List<TransversalKpiHistory> findByCollectedAtAfterOrderByCollectedAtAsc(LocalDateTime collectedAt);
}
