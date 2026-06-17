package com.tfg.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resultado de la consulta de firmware de swarms Aruba.
 * Distingue entre respuesta válida, ausencia de datos y error de API para no
 * convertir un fallo temporal en 0 firmwares pendientes.
 */
public class ArubaFirmwareSwarmsResult {

    public enum Status {
        OK,
        NO_DATA,
        API_ERROR
    }

    private final Status status;
    private final JsonNode payload;
    private final String message;

    private ArubaFirmwareSwarmsResult(Status status, JsonNode payload, String message) {
        this.status = status;
        this.payload = payload;
        this.message = message;
    }

    public static ArubaFirmwareSwarmsResult ok(JsonNode payload) {
        return new ArubaFirmwareSwarmsResult(Status.OK, payload, null);
    }

    public static ArubaFirmwareSwarmsResult noData(String message) {
        return new ArubaFirmwareSwarmsResult(Status.NO_DATA, null, message);
    }

    public static ArubaFirmwareSwarmsResult apiError(String message) {
        return new ArubaFirmwareSwarmsResult(Status.API_ERROR, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasPayload() {
        return Status.OK.equals(status) && payload != null;
    }
}
