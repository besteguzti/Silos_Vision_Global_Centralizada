package com.tfg.dashboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.ArubaSwitch;

public interface ArubaSwitchRepository extends JpaRepository<ArubaSwitch, Long> {

    Optional<ArubaSwitch> findBySerial(String serial);

    Optional<ArubaSwitch> findTopByLastSeenAtIsNotNullOrderByLastSeenAtDesc();
}
