package com.liatrio.dora.controller;

import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.service.CsvExportService;
import com.liatrio.dora.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Tag(name = "Export", description = "Export DORA metrics as CSV")
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 365;
    /** Allow only alphanumeric characters, hyphens, underscores, and dots in filename segments. */
    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]");

    private final MetricsService metricsService;
    private final CsvExportService csvExportService;

    public ExportController(MetricsService metricsService, CsvExportService csvExportService) {
        this.metricsService = metricsService;
        this.csvExportService = csvExportService;
    }

    @Operation(summary = "Export metrics as CSV", description = "Returns a CSV file with summary metrics and weekly timeseries data")
    @GetMapping(value = "/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "30") int days) {

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        }
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be between 7 and 365");
        }

        String token = authHeader.substring(7);
        log.info("CSV export requested for {}/{} over {} days", owner, repo, days);

        MetricsResponse response = metricsService.getMetrics(owner, repo, token, days);
        byte[] csv = csvExportService.generateCsv(response);

        String safeOwner = SAFE_FILENAME.matcher(owner).replaceAll("_");
        String safeRepo  = SAFE_FILENAME.matcher(repo).replaceAll("_");
        String filename = String.format("dora-%s-%s-%s.csv", safeOwner, safeRepo, LocalDate.now());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
