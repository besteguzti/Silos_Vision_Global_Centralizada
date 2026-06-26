package com.tfg.dashboard.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.tfg.dashboard.dto.ArubaApAnnotationDto;
import com.tfg.dashboard.dto.ArubaApAnnotationRequest;
import com.tfg.dashboard.dto.ArubaApInfo;
import com.tfg.dashboard.dto.ArubaInactiveApDto;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.ArubaStoredAccessPointDto;
import com.tfg.dashboard.dto.ArubaStoredSwitchDto;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.dto.ArubaSwitchClientUsageDto;
import com.tfg.dashboard.dto.ArubaWifiClientInfo;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.service.ArubaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Controlador de Aruba Central.
 *
 * Indica el índice de salud Aruba normalizado y endpoints de consulta/sincronización manual. La integración real y la
 * persistencia se mantienen en {@link ArubaService}.
 */
@RestController
@RequestMapping("/aruba")
@Validated
public class ArubaController {

    private final ArubaService service;

    public ArubaController(ArubaService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ArubaSummary getSummary() {
        return service.getSummary();
    }

    @GetMapping("/network-status")
    public ArubaNetworkStatusDto getNetworkStatus() {
        return service.getNetworkStatus();
    }

    @GetMapping("/inactive-aps")
    public List<ArubaInactiveApDto> getInactiveAps() {
        return service.getInactiveAps();
    }

    @PutMapping("/inactive-aps/{serial}/annotation")
    public ArubaApAnnotationDto saveInactiveApAnnotation(
            @PathVariable @NotBlank String serial,
            @Valid @RequestBody ArubaApAnnotationRequest request) {

        return service.saveInactiveApAnnotation(serial, request);
    }

    @GetMapping("/aps")
    public List<ArubaApInfo> getAps() {
        return service.getApsList();
    }

    @GetMapping("/stored-aps")
    public List<ArubaStoredAccessPointDto> getStoredAps() {
        return service.getStoredAccessPoints().stream()
                .map(ArubaStoredAccessPointDto::new)
                .toList();
    }

    @GetMapping("/switches")
    public List<ArubaSwitchInfo> getSwitches() {
        return service.getSwitchesList();
    }

    @GetMapping("/stored-switches")
    public List<ArubaStoredSwitchDto> getStoredSwitches() {
        return service.getStoredSwitches().stream()
                .map(ArubaStoredSwitchDto::new)
                .toList();
    }

    @GetMapping("/switch-client-usage")
    public List<ArubaSwitchClientUsageDto> getSwitchClientUsage() {
        return service.getSwitchClientUsage().stream()
                .map(ArubaSwitchClientUsageDto::new)
                .toList();
    }

    @GetMapping("/wifi-clients")
    public List<ArubaWifiClientInfo> getWifiClients() {
        return service.getWifiClientsList();
    }

    @GetMapping("/wifi-clients/diagnostics")
    public Map<String, Object> getWifiClientDiagnostics() {
        return service.getWifiClientsDiagnostics();
    }

    @PostMapping("/sync-aps")
    public String syncAps() {
        service.syncAccessPoints();
        return "APs sincronizados";
    }

    @PostMapping("/sync-switches")
    public String syncSwitches() {
        service.syncSwitches();
        return "Switches sincronizados";
    }

    @PostMapping("/sync-switch-client-usage")
    public String syncSwitchClientUsage() {
        service.syncSwitchClientUsage();
        return "Uso de interfaces down sincronizado";
    }

    @PostMapping("/sync-all")
    public String syncAll() {
        service.syncAll();
        return "Datos Aruba sincronizados";
    }
}
