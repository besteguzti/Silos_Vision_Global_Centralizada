package com.tfg.dashboard.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tfg.dashboard.dto.ArubaApInfo;
import com.tfg.dashboard.dto.ArubaFirmwareSwarmsResult;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.dto.ArubaWifiClientInfo;
import com.tfg.dashboard.service.ArubaAuthService;

/*
 * Clase que realiza llamadas a la API de Aruba. Se piden APs, switches, firmware y clientes WiFi.
 */
@Component
public class ArubaApiClient {

        private static final Logger log = LoggerFactory.getLogger(ArubaApiClient.class);

        private final ArubaAuthService authService;
        private final RestTemplate restTemplate;

        @Value("${aruba.base.url}")
        private String baseUrl;

        public ArubaApiClient(
                        ArubaAuthService authService,
                        RestTemplateBuilder restTemplateBuilder
        ) {
                this.authService = authService;
                this.restTemplate = restTemplateBuilder
                                .setConnectTimeout(Duration.ofSeconds(10))
                                .setReadTimeout(Duration.ofSeconds(30))
                                .build();
        }

        /**
         * Obneter los Access Points desde Aruba Central.
         */
        public List<ArubaApInfo> getApsList() {

                List<ArubaApInfo> result = new ArrayList<>();

                try {
                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        int offset = 0;
                        int limit = 100;
                        while (true) {
                                String url = baseUrl + "/monitoring/v2/aps" + "?offset=" + offset + "&limit=" + limit;

                                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                                                String.class);

                                JsonNode root = mapper.readTree(response.getBody());

                                JsonNode aps = root.get("aps");

                                if (aps == null || !aps.isArray() || aps.size() == 0) {
                                        break;
                                }

                                for (JsonNode ap : aps) {
                                        ArubaApInfo info = new ArubaApInfo();
                                        info.setName(ap.path("name").asText());
                                        info.setStatus(ap.path("status").asText());
                                        info.setIpAddress(ap.path("ip_address").asText());
                                        info.setPublicIpAddress(ap.path("public_ip_address").asText());
                                        info.setSerial(ap.path("serial").asText());
                                        info.setSite(ap.path("site").asText());
                                        info.setFirmwareVersion(ap.path("firmware_version").asText());
                                        info.setMacaddr(ap.path("macaddr").asText());
                                        info.setSwarmName(ap.path("swarm_name").asText());
                                        info.setLastSeenAt(parseLastSeenAt(ap));
                                        result.add(info);
                                }
                                offset += limit;
                        }

                } catch (Exception e) {
                        log.error("Error obteniendo listado de APs desde Aruba", e);
                        throw new ArubaApiException("API_ERROR obteniendo listado de APs desde Aruba", e);
                }
                return result;
        }

        /**
         * Consultar la información de firmware de los swarms.
         */
        public ArubaFirmwareSwarmsResult getFirmwareSwarms() {

                try {
                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        ArrayNode allSwarms = mapper.createArrayNode();
                        int offset = 0;
                        int limit = 20;
                        while (true) {
                                String url = baseUrl + "/firmware/v1/swarms" + "?offset=" + offset + "&limit=" + limit;
                                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                                                String.class);
                                JsonNode root = mapper.readTree(response.getBody());
                                JsonNode swarms = root.get("swarms");

                                if (swarms == null || !swarms.isArray() || swarms.size() == 0) {
                                        break;
                                }

                                for (JsonNode swarm : swarms) {
                                        allSwarms.add(swarm);
                                }

                                offset += limit;
                        }

                        ObjectNode result = mapper.createObjectNode();
                        result.set("swarms", allSwarms);
                        return allSwarms.size() == 0
                                        ? ArubaFirmwareSwarmsResult.noData("Aruba no devolvio swarms de firmware.")
                                        : ArubaFirmwareSwarmsResult.ok(result);

                } catch (HttpStatusCodeException e) {
                        if (isServiceUnavailable(e)) {
                                log.warn("Firmware de swarms Aruba no disponible temporalmente (503). Se omite este bloque sin detener la sincronizacion.");
                                return ArubaFirmwareSwarmsResult.apiError("Firmware de swarms Aruba no disponible temporalmente (503).");
                        }

                        log.error("Error obteniendo firmware de swarms desde Aruba", e);
                        return ArubaFirmwareSwarmsResult.apiError("Error HTTP obteniendo firmware de swarms desde Aruba.");
                } catch (Exception e) {
                        log.error("Error obteniendo firmware de swarms desde Aruba", e);
                        return ArubaFirmwareSwarmsResult.apiError("Error obteniendo firmware de swarms desde Aruba.");
                }
        }

        // Revisar el firmware de los switches para saber si hay actualizaciones pendientes.
        
        public List<ArubaSwitchInfo> getSwitchesList() {

                List<ArubaSwitchInfo> result = new ArrayList<>();

                try {

                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        int offset = 0;
                        int limit = 100;
                        while (true) {

                                String url = baseUrl + "/firmware/v1/devices" + "?device_type=HP" + "&offset=" + offset
                                                + "&limit=100";
                                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                                                String.class);
                                JsonNode root = mapper.readTree(response.getBody());
                                JsonNode devices = root.get("devices");

                                if (devices == null || !devices.isArray() || devices.size() == 0) {
                                        break;
                                }

                                for (JsonNode device : devices) {
                                        ArubaSwitchInfo info = new ArubaSwitchInfo();
                                        info.setSerial(device.path("serial").asText());
                                        info.setMacAddress(device.path("mac_address").asText());
                                        info.setHostname(device.path("hostname").asText());
                                        info.setModel(device.path("model").asText());
                                        info.setDeviceStatus(device.path("device_status").asText());
                                        info.setUpgradeRequired(device.path("upgrade_required").asBoolean(false));
                                        info.setStatusState(device.path("status").path("state").asText());
                                        result.add(info);
                                }

                                offset += limit;
                        }

                } catch (HttpStatusCodeException e) {
                        if (isServiceUnavailable(e)) {
                                log.warn("Firmware de switches Aruba no disponible temporalmente (503). Se omite este bloque sin detener la sincronizacion.");
                                return result;
                        }

                        log.error("Error obteniendo switches desde Aruba", e);
                        throw new ArubaApiException("API_ERROR obteniendo switches desde Aruba", e);
                } catch (Exception e) {
                        log.error("Error obteniendo switches desde Aruba", e);
                        throw new ArubaApiException("API_ERROR obteniendo switches desde Aruba", e);
                }
                return result;
        }

        //Obtener los switches desde la parte de monitoring de Aruba.

        public List<ArubaSwitchInfo> getMonitoringSwitchesList() {

                List<ArubaSwitchInfo> result = new ArrayList<>();
                try {
                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        int offset = 0;
                        int limit = 100;

                        while (true) {
                                String url = baseUrl + "/monitoring/v1/switches" + "?offset=" + offset + "&limit=" + limit;
                                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                                JsonNode root = mapper.readTree(response.getBody());
                                JsonNode switches = findArray(root, "switches", "devices", "items", "data");

                                if (switches == null || switches.size() == 0) {

                                        if (offset == 0) {

                                                log.warn("La respuesta de monitoring switches no contiene switches. Campos raiz: {}",
                                                                fieldNames(root));
                                        }

                                        break;
                                }

                                for (JsonNode switchNode : switches) {

                                        ArubaSwitchInfo info = new ArubaSwitchInfo();
                                        info.setSerial(text(switchNode, "serial", "serial_number"));
                                        info.setMacAddress(text(switchNode, "macaddr", "mac_address", "mac"));
                                        info.setHostname(text(switchNode, "name", "hostname", "device_name"));
                                        info.setModel(text(switchNode, "model"));
                                        info.setDeviceStatus(text(switchNode, "status", "device_status"));
                                        result.add(info);
                                }

                                offset += limit;
                        }

                } catch (Exception e) {

                        log.error("Error obteniendo switches desde Aruba monitoring", e);
                        throw new ArubaApiException("API_ERROR obteniendo switches desde Aruba monitoring", e);
                }

                return result;
        }

        //Consultar los clientes WiFi conectados.
        
        public List<ArubaWifiClientInfo> getWifiClientsList() {

                return getClientsList("WIRELESS", "clientes WiFi");
        }

        private List<ArubaWifiClientInfo> getClientsList(String clientType,String logLabel) {

                List<ArubaWifiClientInfo> result = new ArrayList<>();

                try {

                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        int offset = 0;
                        int limit = 1000;
                        while (true) {

                                String url = baseUrl
                                                + "/monitoring/v2/clients"
                                                + "?client_type=" + clientType
                                                + "&client_status=CONNECTED"
                                                + "&calculate_total=true"
                                                + "&timerange=3H"
                                                + "&offset=" + offset
                                                + "&limit=" + limit;

                                ResponseEntity<String> response = restTemplate.exchange(
                                                url,
                                                HttpMethod.GET,
                                                entity,
                                                String.class);

                                JsonNode root = mapper.readTree(response.getBody());

                                JsonNode clients = findArray(
                                                root,
                                                "clients",
                                                "client",
                                                "items",
                                                "data");

                                if (clients == null || clients.size() == 0) {

                                        if (offset == 0) {

                                                log.warn(
                                                        "La respuesta de Aruba clients {} no contiene clientes. Campos raiz: {}, total={}, count={}, client_count={}",
                                                        clientType,
                                                        fieldNames(root),
                                                        root.path("total").asText(""),
                                                        root.path("count").asText(""),
                                                        root.path("client_count").asText(""));
                                        }

                                        break;
                                }

                                for (JsonNode client : clients) {

                                        ArubaWifiClientInfo info = new ArubaWifiClientInfo();
                                        info.setAssociatedDevice(text(client, "associated_device"));
                                        info.setAssociatedDeviceMac(text(client, "associated_device_mac"));
                                        info.setAssociatedDeviceName(text(client, "associated_device_name"));
                                        info.setGroupName(text(client, "group_name", "groupName", "group"));
                                        info.setHostname(text(client, "hostname"));
                                        info.setIpAddress(text(client, "ip_address"));
                                        info.setLastConnectionTime(client.path("last_connection_time").asLong(0));
                                        info.setMacaddr(text(client, "macaddr", "mac_address"));
                                        info.setNetwork(text(client, "network", "network_name", "networkName", "ssid", "ssid_name"));
                                        info.setOsType(text(client, "os_type"));
                                        result.add(info);
                                }

                                offset += limit;
                        }

                } catch (Exception e) {

                        log.error("Error obteniendo " + logLabel + " desde Aruba",e);
                        throw new ArubaApiException("API_ERROR obteniendo " + logLabel + " desde Aruba", e);
                }

                log.info("{} obtenidos desde Aruba: {}",logLabel, result.size());
                return result;
        }

        /**
         * Cuenta puertos en estado down de un switch concreto. Si falla la
         * consulta, devuelve 0 para no detener el ciclo de sincronización.
         */
        public int countSwitchPortsDown(String serial) {

                if (serial == null || serial.isBlank()) {
                        return 0;
                }

                try {

                        String token = authService.getAccessToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(token);
                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ObjectMapper mapper = new ObjectMapper();
                        String encodedSerial = URLEncoder.encode(serial, StandardCharsets.UTF_8);

                        String url = baseUrl + "/monitoring/v1/switches/" + encodedSerial + "/ports";

                        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                        JsonNode root = mapper.readTree(response.getBody());
                        
                        return countPortsByStatus(root, "down");

                } catch (Exception e) {
                        log.error("Error obteniendo puertos del switch {} desde Aruba", serial, e);
                        return 0;
                }
        }

        private int countPortsByStatus(JsonNode root,String expectedStatus) {

                JsonNode ports = findArray(root,"ports","interfaces","items","data");

                if (ports == null || !ports.isArray()) {
                        return 0;
                }

                int count = 0;

                for (JsonNode port : ports) {

                        String status = text(port, "status");

                        if (expectedStatus.equalsIgnoreCase(status)) {
                                count++;
                        }
                }

                return count;
        }

        private JsonNode findArray(JsonNode root,String... names) {

                for (String name : names) {

                        JsonNode node = root.get(name);

                        if (node != null && node.isArray()) {
                                return node;
                        }
                }

                return null;
        }

        private String text(JsonNode node,String... names) {

                for (String name : names) {

                        JsonNode value = node.get(name);

                        if (value != null && !value.isNull()) {
                                return value.asText().trim();
                        }
                }
                return "";
        }

        private LocalDateTime parseLastSeenAt(JsonNode node) {

                JsonNode value = firstPresent(
                                node,
                                "last_seen_at",
                                "last_seen",
                                "lastSeenAt",
                                "lastSeen",
                                "last_contact_at",
                                "last_contact",
                                "lastContactAt",
                                "lastContact",
                                "last_contacted_at",
                                "last_contacted",
                                "lastContactedAt",
                                "lastContacted",
                                "last_checkin",
                                "last_check_in",
                                "lastHeartbeat",
                                "last_heartbeat",
                                "last_modified",
                                "lastModified");

                return parseArubaDate(value);
        }

        private JsonNode firstPresent(JsonNode node,String... names) {

                if (node == null) {
                        return null;
                }

                for (String name : names) {

                        JsonNode value = node.get(name);

                        if (value == null || value.isNull() || value.isMissingNode()) {
                                continue;
                        }

                        if (value.isTextual() && value.asText().isBlank()) {
                                continue;
                        }

                        return value;
                }

                return null;
        }

        private LocalDateTime parseArubaDate(JsonNode value) {

                if (value == null || value.isNull() || value.isMissingNode()) {
                        return null;
                }

                if (value.isNumber()) {

                        long raw = value.asLong();

                        if (raw <= 0) {
                                return null;
                        }

                        long epochMillis = raw > 9_999_999_999L ? raw : raw * 1000L;
                        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                }

                if (!value.isTextual()) {
                        return null;
                }

                String text = value.asText();

                if (text == null || text.isBlank()) {
                        return null;
                }

                try {
                        return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
                } catch (DateTimeParseException ignored) {
                        // Algunas respuestas pueden venir sin zona horaria; se prueban formatos locales.
                }

                for (DateTimeFormatter formatter : List.of(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) {

                        try {
                                return LocalDateTime.parse(text, formatter);
                        } catch (DateTimeParseException ignored) {
                                // Se intenta el siguiente formato sin detener la sincronizacion.
                        }
                }

                log.warn("No se pudo interpretar la fecha last seen de Aruba: {}", text);
                return null;
        }

        private List<String> fieldNames(JsonNode node) {

                List<String> names = new ArrayList<>();

                Iterator<String> iterator = node.fieldNames();

                while (iterator.hasNext()) {
                        names.add(iterator.next());
                }
                return names;
        }

        private boolean isServiceUnavailable(HttpStatusCodeException e) {

                return e.getStatusCode().value() == 503;
        }
}

