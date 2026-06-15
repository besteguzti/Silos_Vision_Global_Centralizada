package com.tfg.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tfg.dashboard.model.OAuthToken;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

}