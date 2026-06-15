package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.dashboard.model.CitrixMetricsHistory;

public interface CitrixMetricsHistoryRepository extends JpaRepository<CitrixMetricsHistory, Long> {

    Optional<CitrixMetricsHistory> findTopByOrderByCollectedAtDesc();

    @Transactional
    long deleteByCollectedAtBefore(LocalDateTime cutoff);
}
