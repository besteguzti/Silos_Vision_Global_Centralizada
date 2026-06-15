package com.tfg.dashboard.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.ArubaSwitchClientUsage;

public interface ArubaSwitchClientUsageRepository extends JpaRepository<ArubaSwitchClientUsage, Long> {

        Optional<ArubaSwitchClientUsage> findByAssociatedDevice(String associatedDevice);

        Optional<ArubaSwitchClientUsage> findTopByUpdatedAtIsNotNullOrderByUpdatedAtDesc();

        List<ArubaSwitchClientUsage> findByAssociatedDeviceInOrderByDownInterfacesDescAssociatedDeviceAsc(List<String> associatedDevices);
}
