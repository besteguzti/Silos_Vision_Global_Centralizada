package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.dashboard.model.Microsoft365MetricsHistory;

public interface Microsoft365MetricsHistoryRepository extends JpaRepository<Microsoft365MetricsHistory, Long> {

    Optional<Microsoft365MetricsHistory> findTopByOrderByCollectedAtDesc();

    @Transactional
    long deleteByCollectedAtBefore(LocalDateTime cutoff);
}
