package com.tfg.dashboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.PlatformWeightConfiguration;

public interface PlatformWeightConfigurationRepository extends JpaRepository<PlatformWeightConfiguration, Long> {

    Optional<PlatformWeightConfiguration> findByPlatform(String platform);
}
