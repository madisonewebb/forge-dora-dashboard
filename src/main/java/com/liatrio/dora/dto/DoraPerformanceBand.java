package com.liatrio.dora.dto;

public enum DoraPerformanceBand {

    ELITE, HIGH, MEDIUM, LOW;

    /**
     * Classify deployment frequency.
     * Elite: >= 1/day; High: >= 1/week; Medium: >= 1/month; Low: < 1/month
     */
    public static DoraPerformanceBand forDeploymentFrequency(double deploysPerDay) {
        if (deploysPerDay >= 1.0) return ELITE;
        if (deploysPerDay >= 1.0 / 7)  return HIGH;
        if (deploysPerDay >= 1.0 / 30) return MEDIUM;
        return LOW;
    }

    /**
     * Classify lead time for changes.
     * Elite: < 1h; High: 1h–24h; Medium: 24h–168h; Low: > 168h
     */
    public static DoraPerformanceBand forLeadTime(double hours) {
        if (hours < 1.0)   return ELITE;
        if (hours < 24.0)  return HIGH;
        if (hours < 168.0) return MEDIUM;
        return LOW;
    }

    /**
     * Classify change failure rate.
     * Elite: 0–5%; High: 5–10%; Medium: 10–15%; Low: > 15%
     */
    public static DoraPerformanceBand forChangeFailureRate(double percent) {
        if (percent <= 5.0)  return ELITE;
        if (percent <= 10.0) return HIGH;
        if (percent <= 15.0) return MEDIUM;
        return LOW;
    }

    /**
     * Classify mean time to restore.
     * Elite: < 1h; High: 1h–24h; Medium: 24h–168h; Low: > 168h
     */
    public static DoraPerformanceBand forMttr(double hours) {
        if (hours < 1.0)   return ELITE;
        if (hours < 24.0)  return HIGH;
        if (hours < 168.0) return MEDIUM;
        return LOW;
    }
}
