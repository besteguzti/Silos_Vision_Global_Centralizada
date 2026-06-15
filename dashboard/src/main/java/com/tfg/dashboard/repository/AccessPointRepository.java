package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.AccessPoint;

public interface AccessPointRepository extends JpaRepository<AccessPoint, Long> {

        Optional<AccessPoint> findBySerial(String serial);

        Optional<AccessPoint> findTopByLastSeenAtIsNotNullOrderByLastSeenAtDesc();

        Optional<AccessPoint> findTopByLastSeenAtIsNotNullOrderByLastSeenAtAsc();

        Long countBySerialIsNotNullAndLastSeenAtBefore(LocalDateTime date);

        List<AccessPoint> findBySerialIsNotNullAndLastSeenAtBeforeOrderByLastSeenAtAsc(LocalDateTime date);

        Long countBySerialIsNotNullAndLastSeenAtIsNull();
}
