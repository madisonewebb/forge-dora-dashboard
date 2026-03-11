package com.liatrio.dora.insights;

import com.liatrio.dora.dto.WeekDataPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendDirectionCalculatorTest {

    private final TrendDirectionCalculator calculator = new TrendDirectionCalculator();

    private WeekDataPoint point(double value) {
        return new WeekDataPoint(LocalDate.now(), value);
    }

    @Test
    void calculate_improving_whenSecondHalfAverageExceedsFirstByMoreThan10Percent() {
        // first half avg = 1.0, second half avg = 1.2 (+20%)
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(1.2), point(1.2), point(1.2), point(1.2)
        );
        assertEquals(TrendDirection.IMPROVING, calculator.calculate(series, false));
    }

    @Test
    void calculate_declining_whenSecondHalfAverageIsMoreThan10PercentBelowFirst() {
        // first half avg = 1.0, second half avg = 0.8 (-20%)
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(0.8), point(0.8), point(0.8), point(0.8)
        );
        assertEquals(TrendDirection.DECLINING, calculator.calculate(series, false));
    }

    @Test
    void calculate_stable_whenDeltaIsWithin10Percent() {
        // first half avg = 1.0, second half avg = 1.05 (+5%)
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(1.05), point(1.05), point(1.05), point(1.05)
        );
        assertEquals(TrendDirection.STABLE, calculator.calculate(series, false));
    }

    @Test
    void calculate_stable_whenAllValuesAreZero() {
        List<WeekDataPoint> series = List.of(
                point(0.0), point(0.0), point(0.0), point(0.0)
        );
        assertEquals(TrendDirection.STABLE, calculator.calculate(series, false));
    }

    @Test
    void calculate_stable_whenTimeSeriesHasOnePoint() {
        List<WeekDataPoint> series = List.of(point(5.0));
        assertEquals(TrendDirection.STABLE, calculator.calculate(series, false));
    }

    // Lower-is-better metrics (Lead Time, CFR, MTTR): rising value = DECLINING, falling = IMPROVING

    @Test
    void calculate_declining_whenLowerIsBetterAndValueRises() {
        // CFR doubles (+100%) → bad → DECLINING
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(1.2), point(1.2), point(1.2), point(1.2)
        );
        assertEquals(TrendDirection.DECLINING, calculator.calculate(series, true));
    }

    @Test
    void calculate_improving_whenLowerIsBetterAndValueFalls() {
        // MTTR drops 20% → good → IMPROVING
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(0.8), point(0.8), point(0.8), point(0.8)
        );
        assertEquals(TrendDirection.IMPROVING, calculator.calculate(series, true));
    }

    @Test
    void calculate_stable_whenLowerIsBetterAndDeltaWithin10Percent() {
        List<WeekDataPoint> series = List.of(
                point(1.0), point(1.0), point(1.0), point(1.0),
                point(1.05), point(1.05), point(1.05), point(1.05)
        );
        assertEquals(TrendDirection.STABLE, calculator.calculate(series, true));
    }
}
