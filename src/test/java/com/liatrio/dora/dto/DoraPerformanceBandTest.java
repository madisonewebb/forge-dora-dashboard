package com.liatrio.dora.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoraPerformanceBandTest {

    // ── Deployment Frequency ─────────────────────────────────────────────────

    @Test
    void forDeploymentFrequency_elite_atExactlyOnePerDay() {
        assertEquals(DoraPerformanceBand.ELITE, DoraPerformanceBand.forDeploymentFrequency(1.0));
    }

    @Test
    void forDeploymentFrequency_elite_aboveOnePerDay() {
        assertEquals(DoraPerformanceBand.ELITE, DoraPerformanceBand.forDeploymentFrequency(3.5));
    }

    @Test
    void forDeploymentFrequency_high_onePerWeek() {
        // 1/7 ≈ 0.143 — one per week boundary
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forDeploymentFrequency(1.0 / 7));
    }

    @Test
    void forDeploymentFrequency_medium_justBelowOnePerWeek() {
        // Slightly below 1/7 → MEDIUM
        assertEquals(DoraPerformanceBand.MEDIUM, DoraPerformanceBand.forDeploymentFrequency(0.14));
    }

    @Test
    void forDeploymentFrequency_low_belowOnePerMonth() {
        assertEquals(DoraPerformanceBand.LOW, DoraPerformanceBand.forDeploymentFrequency(0.02));
    }

    // ── Lead Time ────────────────────────────────────────────────────────────

    @Test
    void forLeadTime_elite_under1Hour() {
        assertEquals(DoraPerformanceBand.ELITE, DoraPerformanceBand.forLeadTime(0.5));
    }

    @Test
    void forLeadTime_high_4Hours() {
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forLeadTime(4.0));
    }

    @Test
    void forLeadTime_high_atExactly1Hour() {
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forLeadTime(1.0));
    }

    @Test
    void forLeadTime_medium_2Days() {
        assertEquals(DoraPerformanceBand.MEDIUM, DoraPerformanceBand.forLeadTime(48.0));
    }

    @Test
    void forLeadTime_low_over1Week() {
        assertEquals(DoraPerformanceBand.LOW, DoraPerformanceBand.forLeadTime(200.0));
    }

    // ── Change Failure Rate ───────────────────────────────────────────────────

    @Test
    void forChangeFailureRate_elite_at5Percent() {
        assertEquals(DoraPerformanceBand.ELITE, DoraPerformanceBand.forChangeFailureRate(5.0));
    }

    @Test
    void forChangeFailureRate_high_at51Percent() {
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forChangeFailureRate(5.1));
    }

    @Test
    void forChangeFailureRate_high_at10Percent() {
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forChangeFailureRate(10.0));
    }

    @Test
    void forChangeFailureRate_medium_at12Percent() {
        assertEquals(DoraPerformanceBand.MEDIUM, DoraPerformanceBand.forChangeFailureRate(12.0));
    }

    @Test
    void forChangeFailureRate_low_above15Percent() {
        assertEquals(DoraPerformanceBand.LOW, DoraPerformanceBand.forChangeFailureRate(20.0));
    }

    // ── MTTR ─────────────────────────────────────────────────────────────────

    @Test
    void forMttr_elite_under1Hour() {
        assertEquals(DoraPerformanceBand.ELITE, DoraPerformanceBand.forMttr(0.5));
    }

    @Test
    void forMttr_high_at1Hour() {
        assertEquals(DoraPerformanceBand.HIGH, DoraPerformanceBand.forMttr(1.0));
    }

    @Test
    void forMttr_medium_25Hours() {
        assertEquals(DoraPerformanceBand.MEDIUM, DoraPerformanceBand.forMttr(25.0));
    }

    @Test
    void forMttr_low_over1Week() {
        assertEquals(DoraPerformanceBand.LOW, DoraPerformanceBand.forMttr(200.0));
    }
}
