package com.tfg.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.ArubaNetworkStatusHistory;

public interface ArubaNetworkStatusHistoryRepository extends JpaRepository<ArubaNetworkStatusHistory, Long> {
}
