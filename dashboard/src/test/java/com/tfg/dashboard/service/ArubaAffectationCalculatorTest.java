package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;

class ArubaAffectationCalculatorTest {

        private KpiProperties kpiProperties;

        @BeforeEach
        void setUp() {

                kpiProperties = new KpiProperties();
        }

        @Test
        void allIndicatorsNormalReturnZeroAffectation() {

                ArubaAffectationCalculator.Result result = calculate(baseInput());

                assertThat(result.totalAffection()).isZero();
                assertThat(result.color()).isEqualTo("GREEN");
                assertThat(result.reasons()).isEmpty();
        }

        @Test
        void arubaOpenTicketsAtYellowThresholdAddTwoPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().arubaOpenTickets(100).build());

                assertThat(result.totalAffection()).isEqualTo(2);
                assertThat(result.reasons()).anyMatch(reason -> reason.contains("amarillo"));
        }

        @Test
        void arubaOpenTicketsAtRedThresholdAddFivePoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().arubaOpenTickets(200).build());

                assertThat(result.totalAffection()).isEqualTo(5);
                assertThat(result.reasons()).anyMatch(reason -> reason.contains("rojo"));
        }

        @Test
        void twentyPercentDownApsDoesNotAddAffection() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().totalAps(100).downAps(20).build());

                assertThat(result.totalAffection()).isZero();
                assertThat(result.accessPointColor()).isEqualTo("GREEN");
        }

        @Test
        void fiftyPercentDownApsAddTwentyPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().totalAps(100).downAps(50).build());

                assertThat(result.totalAffection()).isEqualTo(20);
                assertThat(result.accessPointColor()).isEqualTo("YELLOW");
        }

        @Test
        void allDownApsAddEightyPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().totalAps(100).downAps(100).build());

                assertThat(result.totalAffection()).isEqualTo(80);
                assertThat(result.accessPointColor()).isEqualTo("RED");
        }

        @Test
        void twoPendingFirmwareApsAddTwoPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().pendingFirmwareAps(1).build());

                assertThat(result.totalAffection()).isEqualTo(2);
                assertThat(result.accessPointColor()).isEqualTo("YELLOW");
        }

        @Test
        void sixPendingFirmwareApsRemainYellow() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().pendingFirmwareAps(6).build());

                assertThat(result.totalAffection()).isEqualTo(2);
                assertThat(result.accessPointColor()).isEqualTo("YELLOW");
        }

        @Test
        void twentyInactiveApsAddTenPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().inactiveAps(1).build());

                assertThat(result.totalAffection()).isEqualTo(10);
                assertThat(result.accessPointColor()).isEqualTo("YELLOW");
        }

        @Test
        void zeroWifiClientsAddTwentyPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().totalWifiClients(0).build());

                assertThat(result.totalAffection()).isEqualTo(20);
                assertThat(result.accessPointColor()).isEqualTo("RED");
        }

        @Test
        void twoDownSwitchesAddTwentyPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().downSwitches(2).build());

                assertThat(result.totalAffection()).isEqualTo(20);
                assertThat(result.switchColor()).isEqualTo("YELLOW");
        }

        @Test
        void sixDownSwitchesWithTenTotalRemainYellow() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().downSwitches(6).build());

                assertThat(result.totalAffection()).isEqualTo(20);
                assertThat(result.switchColor()).isEqualTo("YELLOW");
        }

        @Test
        void allDownSwitchesAddEightyPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().totalSwitches(10).downSwitches(10).build());

                assertThat(result.totalAffection()).isEqualTo(80);
                assertThat(result.switchColor()).isEqualTo("RED");
        }

        @Test
        void ignoresSwitchUpgradeWhenNoneArePending() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().switchesFirmwareUpgradeRequired(0).build());

                assertThat(result.totalAffection()).isZero();
                assertThat(result.indicatorStatuses())
                                .containsEntry("switchesFirmwareUpgradeRequired", "GREEN");
                assertThat(result.reasons())
                                .noneMatch(reason -> reason.contains("upgrade pendiente"));
        }

        @Test
        void flagsPendingSwitchUpgrade() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().switchesFirmwareUpgradeRequired(1).build());

                assertThat(result.totalAffection()).isEqualTo(2);
                assertThat(result.switchColor()).isEqualTo("YELLOW");
                assertThat(result.indicatorStatuses())
                                .containsEntry("switchesFirmwareUpgradeRequired", "YELLOW");
                assertThat(result.reasons())
                                .anyMatch(reason -> reason.contains("Switches con upgrade pendiente"));
        }

        @Test
        void twoUnderusedSwitchesAddTwoPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().underusedSwitches(2).build());

                assertThat(result.totalAffection()).isEqualTo(2);
                assertThat(result.switchColor()).isEqualTo("YELLOW");
        }

        @Test
        void sixUnderusedSwitchesAddFivePoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder().underusedSwitches(6).build());

                assertThat(result.totalAffection()).isEqualTo(5);
                assertThat(result.switchColor()).isEqualTo("RED");
        }

        @Test
        void combinedYellowCaseAddsFiftySixPoints() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder()
                                .arubaOpenTickets(100)
                                .totalAps(100)
                                .downAps(50)
                                .pendingFirmwareAps(1)
                                .inactiveAps(1)
                                .downSwitches(2)
                                .underusedSwitches(2)
                                .build());

                assertThat(result.totalAffection()).isEqualTo(56);
                assertThat(result.color()).isEqualTo("YELLOW");
        }

        @Test
        void includesSwitchUpgradeInObservedCombination() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder()
                                .pendingFirmwareAps(4)
                                .inactiveAps(21)
                                .downSwitches(5)
                                .switchesFirmwareUpgradeRequired(9)
                                .build());

                assertThat(result.totalAffection()).isEqualTo(34);
                assertThat(result.color()).isEqualTo("YELLOW");
                assertThat(result.reasons())
                                .anyMatch(reason -> reason.contains("Firmware pendiente"))
                                .anyMatch(reason -> reason.contains("APs inactivos"))
                                .anyMatch(reason -> reason.contains("Switches apagados"))
                                .anyMatch(reason -> reason.contains("Switches con upgrade pendiente"));
        }

        @Test
        void exposesIndividualCardStatusesCalculatedInBackend() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder()
                                .arubaOpenTickets(100)
                                .totalAps(100)
                                .downAps(50)
                                .pendingFirmwareAps(1)
                                .inactiveAps(1)
                                .totalWifiClients(0)
                                .downSwitches(2)
                                .switchesFirmwareUpgradeRequired(1)
                                .underusedSwitches(2)
                                .build());

                assertThat(result.indicatorStatuses())
                                .containsEntry("arubaOpenTickets", "YELLOW")
                                .containsEntry("downAps", "YELLOW")
                                .containsEntry("firmwareOutdated", "YELLOW")
                                .containsEntry("inactiveAps", "YELLOW")
                                .containsEntry("totalWifiClients", "RED")
                                .containsEntry("downSwitches", "YELLOW")
                                .containsEntry("switchesFirmwareUpgradeRequired", "YELLOW")
                                .containsEntry("underusedSwitches", "YELLOW")
                                .containsEntry("totalAps", "NEUTRAL")
                                .containsEntry("totalSwitches", "NEUTRAL");
        }

        @Test
        void zeroMutualiaCriticalNetworksExposeRedStatuses() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder()
                                .mutualiaApsClients(0)
                                .mutualiaWifiClients(0)
                                .build());

                assertThat(result.indicatorStatuses())
                                .containsEntry("mutualiaApsClients", "RED")
                                .containsEntry("mutualiaWifiClients", "RED");
                assertThat(result.reasons())
                                .anyMatch(reason -> reason.contains("Mutualia-APS"))
                                .anyMatch(reason -> reason.contains("Mutualia-WIFI"));
                assertThat(result.totalAffection()).isEqualTo(20);
        }

        @Test
        void individualCardStatusesUseConfiguredThresholds() {

                kpiProperties.getAruba().setPendingFirmwareApsYellowMin(4);

                ArubaAffectationCalculator.Result belowConfiguredThreshold = calculate(inputBuilder()
                                .pendingFirmwareAps(2)
                                .build());

                assertThat(belowConfiguredThreshold.indicatorStatuses())
                                .containsEntry("firmwareOutdated", "GREEN");

                kpiProperties.getAruba().setPendingFirmwareApsYellowMin(2);

                ArubaAffectationCalculator.Result atConfiguredThreshold = calculate(inputBuilder()
                                .pendingFirmwareAps(2)
                                .build());

                assertThat(atConfiguredThreshold.indicatorStatuses())
                                .containsEntry("firmwareOutdated", "YELLOW");
        }

        @Test
        void finalAffectationIsCappedAtOneHundred() {

                ArubaAffectationCalculator.Result result = calculate(inputBuilder()
                                .totalAps(100)
                                .downAps(100)
                                .totalWifiClients(0)
                                .totalSwitches(10)
                                .downSwitches(10)
                                .build());

                assertThat(result.totalAffection()).isEqualTo(100);
                assertThat(result.color()).isEqualTo("RED");
        }

        private ArubaAffectationCalculator.Result calculate(ArubaAffectationCalculator.Input input) {

                return ArubaAffectationCalculator.calculate(input, kpiProperties);
        }

        private ArubaAffectationCalculator.Input baseInput() {

                return inputBuilder().build();
        }

        private InputBuilder inputBuilder() {

                return new InputBuilder();
        }

        private static class InputBuilder {

                private int totalAps = 100;
                private int downAps = 0;
                private int inactiveAps = 0;
                private int pendingFirmwareAps = 0;
                private int totalWifiClients = 50;
                private int mutualiaApsClients = 10;
                private int mutualiaWifiClients = 10;
                private int totalSwitches = 10;
                private int downSwitches = 0;
                private int switchesFirmwareUpgradeRequired = 0;
                private int underusedSwitches = 0;
                private int arubaOpenTickets = 0;

                InputBuilder totalAps(int totalAps) {
                        this.totalAps = totalAps;
                        return this;
                }

                InputBuilder downAps(int downAps) {
                        this.downAps = downAps;
                        return this;
                }

                InputBuilder inactiveAps(int inactiveAps) {
                        this.inactiveAps = inactiveAps;
                        return this;
                }

                InputBuilder pendingFirmwareAps(int pendingFirmwareAps) {
                        this.pendingFirmwareAps = pendingFirmwareAps;
                        return this;
                }

                InputBuilder totalWifiClients(int totalWifiClients) {
                        this.totalWifiClients = totalWifiClients;
                        return this;
                }

                InputBuilder mutualiaApsClients(int mutualiaApsClients) {
                        this.mutualiaApsClients = mutualiaApsClients;
                        return this;
                }

                InputBuilder mutualiaWifiClients(int mutualiaWifiClients) {
                        this.mutualiaWifiClients = mutualiaWifiClients;
                        return this;
                }

                InputBuilder totalSwitches(int totalSwitches) {
                        this.totalSwitches = totalSwitches;
                        return this;
                }

                InputBuilder downSwitches(int downSwitches) {
                        this.downSwitches = downSwitches;
                        return this;
                }

                InputBuilder switchesFirmwareUpgradeRequired(int switchesFirmwareUpgradeRequired) {
                        this.switchesFirmwareUpgradeRequired = switchesFirmwareUpgradeRequired;
                        return this;
                }

                InputBuilder underusedSwitches(int underusedSwitches) {
                        this.underusedSwitches = underusedSwitches;
                        return this;
                }

                InputBuilder arubaOpenTickets(int arubaOpenTickets) {
                        this.arubaOpenTickets = arubaOpenTickets;
                        return this;
                }

                ArubaAffectationCalculator.Input build() {

                        return new ArubaAffectationCalculator.Input(
                                        totalAps,
                                        downAps,
                                        inactiveAps,
                                        pendingFirmwareAps,
                                        totalWifiClients,
                                        mutualiaApsClients,
                                        mutualiaWifiClients,
                                        totalSwitches,
                                        downSwitches,
                                        switchesFirmwareUpgradeRequired,
                                        underusedSwitches,
                                        arubaOpenTickets);
                }
        }
}
