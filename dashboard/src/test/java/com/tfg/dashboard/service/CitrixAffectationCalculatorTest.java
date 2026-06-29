package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;

class CitrixAffectationCalculatorTest {

        private KpiProperties kpiProperties;

        @BeforeEach
        void setUp() {

                kpiProperties = new KpiProperties();
        }

        @Test
        void serverLoadAtSixtySevenPercentReturnsGreen() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(67)
                                .build());

                assertThat(result.percentage()).isZero();
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("GREEN");
                                        assertThat(indicator.getAffectionPercent()).isZero();
                                });
        }

        @Test
        void serverLoadAtEightyPercentReturnsYellow() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(80)
                                .build());

                assertThat(result.percentage()).isEqualTo(15);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("YELLOW");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(15);
                                });
        }

        @Test
        void serverLoadAtEightyNinePercentReturnsYellow() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(89)
                                .build());

                assertThat(result.percentage()).isEqualTo(15);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> assertThat(indicator.getColor()).isEqualTo("YELLOW"));
        }

        @Test
        void serverLoadAtNinetyPercentReturnsRed() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(90)
                                .build());

                assertThat(result.percentage()).isEqualTo(40);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("RED");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(40);
                                });
        }

        @Test
        void averageLogonFortyWithGreenServerLoadReturnsFifteen() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .averageLogonDurationSeconds(40)
                                .serverLoadPercent(53)
                                .build());

                assertThat(result.percentage()).isEqualTo(15);
        }

        @Test
        void averageLogonSixtyOneReturnsEighty() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .averageLogonDurationSeconds(61)
                                .build());

                assertThat(result.percentage()).isEqualTo(70);
        }

        @Test
        void allIndicatorsCorrectReturnsZero() {

                CitrixAffectationCalculator.Result result = calculate(baseInput());

                assertThat(result.percentage()).isZero();
        }

        @Test
        void combinedIndicatorsAboveOneHundredAreClampedToOneHundred() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .averageLogonDurationSeconds(61)
                                .serverLoadPercent(100)
                                .failedLogons(31)
                                .build());

                assertThat(result.percentage()).isEqualTo(100);
        }

        @Test
        void keepsHealthyCitrixCaptureAtZeroAffection() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .activeLicenses(510)
                                .availableDeliveryControllers(3)
                                .totalDeliveryControllers(4)
                                .disconnectedSessions(0)
                                .averageLogonDurationSeconds(12)
                                .serverLoadPercent(53)
                                .failedLogons(4)
                                .citrixOpenTickets(22)
                                .build());

                assertThat(result.percentage()).isZero();
        }

        @Test
        void calculatesCitrixAffectationWithoutForcingMaximumValue() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .activeSessions(42)
                                .activeLicenses(580)
                                .availableDeliveryControllers(3)
                                .totalDeliveryControllers(4)
                                .disconnectedSessions(0)
                                .averageLogonDurationSeconds(21)
                                .serverLoadPercent(75)
                                .failedLogons(7)
                                .citrixOpenTickets(26)
                                .build());

                assertThat(result.percentage()).isEqualTo(30);
                assertThat(result.percentage()).isLessThan(100);
                assertThat(result.color()).isEqualTo("GREEN");
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("GREEN");
                                        assertThat(indicator.getAffectionPercent()).isZero();
                                });
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Average Logon Duration".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> assertThat(indicator.getColor()).isEqualTo("YELLOW"));
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Errores de inicio".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> assertThat(indicator.getColor()).isEqualTo("YELLOW"));
        }

        @Test
        void usesTotalAffectionForTopStatus() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(92)
                                .disconnectedSessions(11)
                                .build());

                assertThat(result.percentage()).isEqualTo(48);
                assertThat(result.color()).isEqualTo("YELLOW");
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Carga de servidores".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> assertThat(indicator.getColor()).isEqualTo("RED"));
        }

        @Test
        void topStatusAtSixtySixPercentIsYellow() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(98)
                                .disconnectedSessions(11)
                                .build());

                assertThat(result.percentage()).isEqualTo(66);
                assertThat(result.color()).isEqualTo("YELLOW");
        }

        @Test
        void topStatusAtSixtySevenPercentIsRed() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(99)
                                .build());

                assertThat(result.percentage()).isEqualTo(67);
                assertThat(result.color()).isEqualTo("RED");
        }

        @Test
        void topStatusAtSeventyPercentIsRed() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .serverLoadPercent(100)
                                .build());

                assertThat(result.percentage()).isEqualTo(70);
                assertThat(result.color()).isEqualTo("RED");
        }

        @Test
        void threeOfFourDeliveryControllersReturnsGreenAndZeroAffection() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .availableDeliveryControllers(3)
                                .totalDeliveryControllers(4)
                                .build());

                assertThat(result.percentage()).isZero();
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Delivery Controllers disponibles".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("GREEN");
                                        assertThat(indicator.getAffectionPercent()).isZero();
                                });
        }

        @Test
        void twoOfFourDeliveryControllersReturnsYellowAndAddsFifteenPercent() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .availableDeliveryControllers(2)
                                .totalDeliveryControllers(4)
                                .build());

                assertThat(result.percentage()).isEqualTo(15);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Delivery Controllers disponibles".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("YELLOW");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(15);
                                });
        }

        @Test
        void oneOfFourDeliveryControllersReturnsRedAndAddsSeventyPercent() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .availableDeliveryControllers(1)
                                .totalDeliveryControllers(4)
                                .build());

                assertThat(result.percentage()).isEqualTo(70);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Delivery Controllers disponibles".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("RED");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(70);
                                });
        }

        @Test
        void treatsZeroActiveSessionsAsMaximumAffection() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .activeSessions(0)
                                .build());

                assertThat(result.percentage()).isEqualTo(100);
                assertThat(result.reasons()).anyMatch(reason -> reason.contains("No hay sesiones activas"));
                assertThat(result.affectedService()).isTrue();
                assertThat(result.criticalCondition()).isTrue();
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Sesiones activas".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("RED");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(100);
                                });
        }

        @Test
        void treatsZeroDeliveryControllersAsMaximumAffection() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .availableDeliveryControllers(0)
                                .totalDeliveryControllers(4)
                                .build());

                assertThat(result.percentage()).isEqualTo(100);
                assertThat(result.color()).isEqualTo("RED");
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Delivery Controllers disponibles".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("RED");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(100);
                                });
        }

        @Test
        void flagsModerateFailedLogonsAsYellow() {

                CitrixAffectationCalculator.Result result = calculate(inputBuilder()
                                .failedLogons(8)
                                .build());

                assertThat(result.percentage()).isEqualTo(15);
                assertThat(result.indicators())
                                .filteredOn(indicator -> "Errores de inicio".equals(indicator.getName()))
                                .singleElement()
                                .satisfies(indicator -> {
                                        assertThat(indicator.getColor()).isEqualTo("YELLOW");
                                        assertThat(indicator.getAffectionPercent()).isEqualTo(15);
                                        assertThat(indicator.getReason()).contains("Entre 6 y 20 errores de inicio");
                                });
                assertThat(result.reasons())
                                .anyMatch(reason -> reason.toLowerCase().contains("errores de inicio"));
        }

        private CitrixAffectationCalculator.Result calculate(CitrixAffectationCalculator.Input input) {

                return CitrixAffectationCalculator.calculate(input, kpiProperties);
        }

        private CitrixAffectationCalculator.Input baseInput() {

                return inputBuilder().build();
        }

        private InputBuilder inputBuilder() {

                return new InputBuilder();
        }

        private static class InputBuilder {

                private int activeSessions = 100;
                private int activeLicenses = 500;
                private int availableDeliveryControllers = 4;
                private int totalDeliveryControllers = 4;
                private int disconnectedSessions = 0;
                private int averageLogonDurationSeconds = 10;
                private int serverLoadPercent = 20;
                private int failedLogons = 0;
                private int citrixOpenTickets = 0;

                InputBuilder activeSessions(int activeSessions) {
                        this.activeSessions = activeSessions;
                        return this;
                }

                InputBuilder activeLicenses(int activeLicenses) {
                        this.activeLicenses = activeLicenses;
                        return this;
                }

                InputBuilder availableDeliveryControllers(int availableDeliveryControllers) {
                        this.availableDeliveryControllers = availableDeliveryControllers;
                        return this;
                }

                InputBuilder totalDeliveryControllers(int totalDeliveryControllers) {
                        this.totalDeliveryControllers = totalDeliveryControllers;
                        return this;
                }

                InputBuilder disconnectedSessions(int disconnectedSessions) {
                        this.disconnectedSessions = disconnectedSessions;
                        return this;
                }

                InputBuilder averageLogonDurationSeconds(int averageLogonDurationSeconds) {
                        this.averageLogonDurationSeconds = averageLogonDurationSeconds;
                        return this;
                }

                InputBuilder serverLoadPercent(int serverLoadPercent) {
                        this.serverLoadPercent = serverLoadPercent;
                        return this;
                }

                InputBuilder failedLogons(int failedLogons) {
                        this.failedLogons = failedLogons;
                        return this;
                }

                InputBuilder citrixOpenTickets(int citrixOpenTickets) {
                        this.citrixOpenTickets = citrixOpenTickets;
                        return this;
                }

                CitrixAffectationCalculator.Input build() {
                        return new CitrixAffectationCalculator.Input(
                                        activeSessions,
                                        activeLicenses,
                                        availableDeliveryControllers,
                                        totalDeliveryControllers,
                                        disconnectedSessions,
                                        averageLogonDurationSeconds,
                                        serverLoadPercent,
                                        failedLogons,
                                        citrixOpenTickets);
                }
        }
}
