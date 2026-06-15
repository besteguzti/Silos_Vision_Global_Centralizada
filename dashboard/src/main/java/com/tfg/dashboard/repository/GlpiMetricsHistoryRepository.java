package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.dashboard.model.GlpiMetricsHistory;

public interface GlpiMetricsHistoryRepository extends JpaRepository<GlpiMetricsHistory, Long> {

    Optional<GlpiMetricsHistory> findTopByOrderByCollectedAtDesc();

    List<GlpiMetricsHistory> findByCollectedAtAfterOrderByCollectedAtAsc(LocalDateTime since);

    @Transactional
    long deleteByCollectedAtBefore(LocalDateTime cutoff);
}
