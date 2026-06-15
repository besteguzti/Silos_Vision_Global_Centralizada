package com.tfg.dashboard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.dashboard.model.ArubaApAnnotation;

public interface ArubaApAnnotationRepository extends JpaRepository<ArubaApAnnotation, Long> {

    Optional<ArubaApAnnotation> findBySerial(String serial);

    List<ArubaApAnnotation> findBySerialIn(Collection<String> serials);
}
