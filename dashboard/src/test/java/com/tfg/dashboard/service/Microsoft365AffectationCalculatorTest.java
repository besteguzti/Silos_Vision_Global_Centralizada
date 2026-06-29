package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.Microsoft365IndicatorStatusDto;

class Microsoft365AffectationCalculatorTest {

    private KpiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KpiProperties();
    }

    @Test
    void allIndicatorsNormalReturnZeroAffection() {
        Microsoft365AffectationCalculator.Result result =
                Microsoft365AffectationCalculator.calculate(healthyInput(), properties);

        assertThat(result.percentage()).isZero();
        assertThat(result.color()).isEqualTo("GREEN");
    }

    @Test
    void unassignedLicensesUseYellowAndRedPartialAffection() {
        Microsoft365AffectationCalculator.Result yellow =
                Microsoft365AffectationCalculator.calculate(
                        input().unassignedLicenses(19).build(),
                        properties);
        Microsoft365AffectationCalculator.Result red =
                Microsoft365AffectationCalculator.calculate(
                        input().unassignedLicenses(2).build(),
                        properties);

        assertIndicator(yellow, "Licencias no asignadas", "YELLOW", 2);
        assertThat(yellow.percentage()).isEqualTo(2);
        assertIndicator(red, "Licencias no asignadas", "RED", 5);
        assertThat(red.percentage()).isEqualTo(5);
    }

    @Test
    void serviceIncidentsUseTwentyAndThirtyPartialAffection() {
        Microsoft365AffectationCalculator.Result degraded =
                Microsoft365AffectationCalculator.calculate(
                        input().outlookStatus("DEGRADED").build(),
                        properties);
        Microsoft365AffectationCalculator.Result incident =
                Microsoft365AffectationCalculator.calculate(
                        input().teamsStatus("INCIDENT").build(),
                        properties);

        assertIndicator(degraded, "Outlook", "YELLOW", 20);
        assertThat(degraded.percentage()).isEqualTo(20);
        assertIndicator(incident, "Teams", "RED", 30);
        assertThat(incident.percentage()).isEqualTo(30);
    }

    @Test
    void sharePointStorageUsesTenAndFiftyPartialAffection() {
        Microsoft365AffectationCalculator.Result yellow =
                Microsoft365AffectationCalculator.calculate(
                        input().sharePointStoragePercent(80).build(),
                        properties);
        Microsoft365AffectationCalculator.Result red =
                Microsoft365AffectationCalculator.calculate(
                        input().sharePointStoragePercent(90).build(),
                        properties);

        assertIndicator(yellow, "Almacenamiento de SharePoint", "YELLOW", 10);
        assertThat(yellow.percentage()).isEqualTo(10);
        assertIndicator(red, "Almacenamiento de SharePoint", "RED", 50);
        assertThat(red.percentage()).isEqualTo(50);
    }

    @Test
    void securityAndIdentityIndicatorsUseConfiguredPartials() {
        Microsoft365AffectationCalculator.Result result =
                Microsoft365AffectationCalculator.calculate(
                        input()
                                .riskyUsers(10)
                                .failedSignIns(20)
                                .usersWithoutMfa(5)
                                .appsSecretsExpiringSoon(1)
                                .unusedApplications(1)
                                .highPrivilegeApplications(1)
                                .build(),
                        properties);

        assertIndicator(result, "Usuarios en riesgo", "RED", 5);
        assertIndicator(result, "Inicios fallidos", "RED", 10);
        assertIndicator(result, "Usuarios sin MFA", "RED", 10);
        assertIndicator(result, "Secretos proximos a caducar", "YELLOW", 2);
        assertIndicator(result, "Aplicaciones sin uso", "YELLOW", 2);
        assertIndicator(result, "Apps permisos elevados", "YELLOW", 2);
        assertThat(result.percentage()).isEqualTo(31);
    }

    @Test
    void deviceAndTicketIndicatorsUseRequestedPartials() {
        Microsoft365AffectationCalculator.Result result =
                Microsoft365AffectationCalculator.calculate(
                        input()
                                .nonCompliantDevices(51)
                                .microsoft365OpenTickets(200)
                                .outdatedWindowsDevices(1)
                                .devicesWithoutEncryption(1)
                                .staleDevices(1)
                                .build(),
                        properties);

        assertIndicator(result, "Equipos no conformes", "RED", 10);
        assertIndicator(result, "Tickets abiertos Microsoft 365", "RED", 5);
        assertIndicator(result, "Windows desactualizados", "YELLOW", 2);
        assertIndicator(result, "Equipos sin cifrado", "RED", 30);
        assertIndicator(result, "Sin check-in >90 dias", "RED", 30);
        assertThat(result.percentage()).isEqualTo(77);
        assertThat(result.color()).isEqualTo("RED");
    }

    @Test
    void combinedExtremeScenarioIsCappedAtOneHundred() {
        Microsoft365AffectationCalculator.Result result =
                Microsoft365AffectationCalculator.calculate(
                        input()
                                .unassignedLicenses(0)
                                .outlookStatus("INCIDENT")
                                .teamsStatus("INCIDENT")
                                .sharePointStatus("INCIDENT")
                                .nearlyFullMailboxes(1)
                                .emailsQuarantined(1)
                                .sharePointStoragePercent(95)
                                .riskyUsers(10)
                                .failedSignIns(20)
                                .usersWithoutMfa(5)
                                .appsSecretsExpiringSoon(1)
                                .unusedApplications(1)
                                .highPrivilegeApplications(1)
                                .nonCompliantDevices(51)
                                .microsoft365OpenTickets(200)
                                .outdatedWindowsDevices(1)
                                .devicesWithoutEncryption(1)
                                .staleDevices(1)
                                .build(),
                        properties);

        assertThat(result.percentage()).isEqualTo(100);
        assertThat(result.color()).isEqualTo("RED");
    }

    private Microsoft365AffectationCalculator.Input healthyInput() {
        return input().build();
    }

    private void assertIndicator(
            Microsoft365AffectationCalculator.Result result,
            String name,
            String status,
            int affection) {

        Microsoft365IndicatorStatusDto indicator =
                result.indicators().stream()
                        .filter(current -> name.equals(current.getName()))
                        .findFirst()
                        .orElseThrow();

        assertThat(indicator.getColor()).isEqualTo(status);
        assertThat(indicator.getAffectionPercent()).isEqualTo(affection);
    }

    private InputBuilder input() {
        return new InputBuilder();
    }

    private static class InputBuilder {
        private int activeUsers = 100;
        private int unassignedLicenses = 20;
        private String outlookStatus = "HEALTHY";
        private String teamsStatus = "HEALTHY";
        private String sharePointStatus = "HEALTHY";
        private int nearlyFullMailboxes = 0;
        private int emailsQuarantined = 0;
        private int sharePointStoragePercent = 79;
        private int riskyUsers = 0;
        private int failedSignIns = 0;
        private int usersWithoutMfa = 0;
        private int appsSecretsExpiringSoon = 0;
        private int unusedApplications = 0;
        private int highPrivilegeApplications = 0;
        private int nonCompliantDevices = 30;
        private int microsoft365OpenTickets = 0;
        private int outdatedWindowsDevices = 0;
        private int devicesWithoutEncryption = 0;
        private int staleDevices = 0;

        InputBuilder unassignedLicenses(int value) {
            this.unassignedLicenses = value;
            return this;
        }

        InputBuilder outlookStatus(String value) {
            this.outlookStatus = value;
            return this;
        }

        InputBuilder teamsStatus(String value) {
            this.teamsStatus = value;
            return this;
        }

        InputBuilder sharePointStatus(String value) {
            this.sharePointStatus = value;
            return this;
        }

        InputBuilder nearlyFullMailboxes(int value) {
            this.nearlyFullMailboxes = value;
            return this;
        }

        InputBuilder emailsQuarantined(int value) {
            this.emailsQuarantined = value;
            return this;
        }

        InputBuilder sharePointStoragePercent(int value) {
            this.sharePointStoragePercent = value;
            return this;
        }

        InputBuilder riskyUsers(int value) {
            this.riskyUsers = value;
            return this;
        }

        InputBuilder failedSignIns(int value) {
            this.failedSignIns = value;
            return this;
        }

        InputBuilder usersWithoutMfa(int value) {
            this.usersWithoutMfa = value;
            return this;
        }

        InputBuilder appsSecretsExpiringSoon(int value) {
            this.appsSecretsExpiringSoon = value;
            return this;
        }

        InputBuilder unusedApplications(int value) {
            this.unusedApplications = value;
            return this;
        }

        InputBuilder highPrivilegeApplications(int value) {
            this.highPrivilegeApplications = value;
            return this;
        }

        InputBuilder nonCompliantDevices(int value) {
            this.nonCompliantDevices = value;
            return this;
        }

        InputBuilder microsoft365OpenTickets(int value) {
            this.microsoft365OpenTickets = value;
            return this;
        }

        InputBuilder outdatedWindowsDevices(int value) {
            this.outdatedWindowsDevices = value;
            return this;
        }

        InputBuilder devicesWithoutEncryption(int value) {
            this.devicesWithoutEncryption = value;
            return this;
        }

        InputBuilder staleDevices(int value) {
            this.staleDevices = value;
            return this;
        }

        Microsoft365AffectationCalculator.Input build() {
            return new Microsoft365AffectationCalculator.Input(
                    activeUsers,
                    unassignedLicenses,
                    outlookStatus,
                    teamsStatus,
                    sharePointStatus,
                    nearlyFullMailboxes,
                    emailsQuarantined,
                    sharePointStoragePercent,
                    riskyUsers,
                    failedSignIns,
                    usersWithoutMfa,
                    appsSecretsExpiringSoon,
                    unusedApplications,
                    highPrivilegeApplications,
                    nonCompliantDevices,
                    microsoft365OpenTickets,
                    outdatedWindowsDevices,
                    devicesWithoutEncryption,
                    staleDevices);
        }
    }
}
