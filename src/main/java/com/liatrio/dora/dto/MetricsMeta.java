package com.liatrio.dora.dto;

import java.time.Instant;

public record MetricsMeta(String owner, String repo, int windowDays, Instant generatedAt) {
}
