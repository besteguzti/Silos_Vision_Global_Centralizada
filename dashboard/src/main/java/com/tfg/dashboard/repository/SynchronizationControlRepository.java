package com.tfg.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.SynchronizationControl;

public interface SynchronizationControlRepository
        extends JpaRepository<SynchronizationControl, Long> {
}
