package com.tfg.dashboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.KpiThresholdConfiguration;

public interface KpiThresholdConfigurationRepository extends JpaRepository<KpiThresholdConfiguration, Long> {

    Optional<KpiThresholdConfiguration> findByConfigKey(String configKey);
}
