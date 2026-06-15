package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.dashboard.model.AnalysisSnapshot;

public interface AnalysisSnapshotRepository extends JpaRepository<AnalysisSnapshot, Long> {

        List<AnalysisSnapshot> findByTimestampAfterOrderByTimestampAsc(LocalDateTime timestamp);

        long countByTimestampAfter(LocalDateTime timestamp);

        List<AnalysisSnapshot> findTop5ByOrderByTimestampDesc();

        @Query("""
                        select count(snapshot) > 0
                        from AnalysisSnapshot snapshot
                        where snapshot.generatedScenario = true
                        and snapshot.timestamp = :timestamp
                        """)
        boolean existsGeneratedScenarioAt(@Param("timestamp") LocalDateTime timestamp);
}
