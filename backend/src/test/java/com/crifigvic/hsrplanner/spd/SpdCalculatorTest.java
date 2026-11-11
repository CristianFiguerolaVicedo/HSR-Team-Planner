package com.crifigvic.hsrplanner.spd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SpdCalculatorTest {

    private static final double DELTA = 1e-6;

    private final SpdCalculator calculator = new SpdCalculator();

    @Nested
    @DisplayName("Action value")
    class ActionValue {

        @Test
        void hundredSpdActsEveryHundredAv() {
            assertEquals(100, calculator.actionValue(100), DELTA);
        }

        @Test
        void fasterUnitsHaveLowerActionValue() {
            assertEquals(62.5, calculator.actionValue(160), DELTA);
        }
    }

    @Nested
    @DisplayName("Cycle length")
    class CycleLength {

        @Test
        void firstCycleLastsOneHundredFiftyAv() {
            assertEquals(150, calculator.cycleEndAv(0), DELTA);
        }

        @Test
        void laterCyclesAddOneHundredAvEach() {
            assertEquals(250, calculator.cycleEndAv(1), DELTA);
            assertEquals(350, calculator.cycleEndAv(2), DELTA);
        }
    }

    @Nested
    @DisplayName("Actions in cycle 0")
    class CycleZero {

        @Test
        void slowUnitDoesNotActInCycleZero() {
            // Needs 10000 / 150 = 66.67 SPD for its first action
            assertEquals(0, calculator.actionsInCycle(66, 0));
        }

        @Test
        void baseSpeedUnitActsOnce() {
            assertEquals(1, calculator.actionsInCycle(100, 0));
        }

        @Test
        void justBelowTheBreakpointStillActsOnce() {
            assertEquals(1, calculator.actionsInCycle(133.3, 0));
        }

        @Test
        void oneHundredThirtyFourSpdActsTwice() {
            assertEquals(2, calculator.actionsInCycle(134, 0));
        }

        @Test
        void exactlyOnTheBreakpointCountsTheAction() {
            double exact = 2 * 10_000.0 / 150; // 133.333...
            assertEquals(2, calculator.actionsInCycle(exact, 0));
        }
    }

    @Nested
    @DisplayName("Known community breakpoints")
    class KnownBreakpoints {

        @ParameterizedTest(name = "{0} SPD -> {2} actions by the end of cycle {1}")
        @CsvSource({
                "120, 1, 3",
                "134, 0, 2",
                "143, 2, 5",
                "160, 1, 4",
                "172, 2, 6",
                "200, 0, 3"
        })
        void reachesTheExpectedNumberOfActions(double spd, int cycle, int expectedActions) {
            assertEquals(expectedActions, calculator.actionsUntilEndOfCycle(spd, cycle));
        }

        @ParameterizedTest(name = "{0} actions by cycle {1} need {2} SPD")
        @CsvSource({
                "2, 0, 133.333333",
                "3, 1, 120.0",
                "4, 1, 160.0",
                "5, 2, 142.857143",
                "6, 2, 171.428571"
        })
        void computesTheBreakpoint(int actions, int cycle, double expectedSpd) {
            assertEquals(expectedSpd, calculator.breakpoint(actions, cycle), DELTA);
        }
    }

    @Nested
    @DisplayName("Next breakpoint")
    class NextBreakpoint {

        @Test
        void fromOneHundredTwentySpdTheNextCycleZeroBreakpointIsTwoActions() {
            assertEquals(133.333333, calculator.nextBreakpoint(120, 0), DELTA);
            assertEquals(13.333333, calculator.spdToNextBreakpoint(120, 0), DELTA);
        }

        @Test
        void sittingOnABreakpointPointsToTheFollowingOne() {
            assertEquals(200, calculator.nextBreakpoint(134, 0), DELTA);
            assertEquals(66, calculator.spdToNextBreakpoint(134, 0), DELTA);
        }

        @ParameterizedTest
        @ValueSource(doubles = {95, 100, 120, 133.34, 142.86, 160, 171.43, 250})
        void missingSpdIsAlwaysPositive(double spd) {
            for (int cycle = 0; cycle < 5; cycle++) {
                assertTrue(calculator.spdToNextBreakpoint(spd, cycle) > 0);
            }
        }
    }

    @Nested
    @DisplayName("Full analysis")
    class Analysis {

        @Test
        void actionsPerCycleAddUpToTheCumulativeTotal() {
            SpdAnalysis analysis = calculator.analyze(143, 5);

            int sum = 0;
            for (SpdAnalysis.CycleBreakdown cycle : analysis.cycles()) {
                sum += cycle.actions();
                assertEquals(sum, cycle.totalActions());
            }
            assertEquals(5, analysis.cycles().size());
        }

        @Test
        void cycleZeroSummaryMatchesTheDirectCalls() {
            SpdAnalysis analysis = calculator.analyze(134, 3);

            assertEquals(74.626866, analysis.actionValue(), DELTA);
            assertEquals(2, analysis.cycleZero().actions());
            assertEquals(200, analysis.cycleZero().nextBreakpoint(), DELTA);
        }
    }

    @Nested
    @DisplayName("Invalid input")
    class InvalidInput {

        @ParameterizedTest
        @ValueSource(doubles = {0, -10, 0.5, 1001, Double.NaN, Double.POSITIVE_INFINITY})
        void rejectsSpeedsOutOfRange(double spd) {
            assertThrows(IllegalArgumentException.class, () -> calculator.analyze(spd, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 31})
        void rejectsCycleCountsOutOfRange(int cycles) {
            assertThrows(IllegalArgumentException.class, () -> calculator.analyze(120, cycles));
        }

        @Test
        void rejectsNegativeCycleIndex() {
            assertThrows(IllegalArgumentException.class, () -> calculator.cycleEndAv(-1));
        }

        @Test
        void rejectsBreakpointsForZeroActions() {
            assertThrows(IllegalArgumentException.class, () -> calculator.breakpoint(0, 0));
        }
    }
}
