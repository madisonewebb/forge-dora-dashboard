package com.liatrio.dora.controller;

import com.liatrio.dora.service.GitHubCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Cache", description = "GitHub data cache management")
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    private final GitHubCacheService cacheService;

    public CacheController(GitHubCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Operation(summary = "Invalidate cache", description = "Clears cached GitHub data for the given repository. Requires Bearer token.")
    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Void> invalidateCache(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        }
        log.info("Manual cache invalidation requested for {}/{}", owner, repo);
        cacheService.invalidate(owner, repo);
        return ResponseEntity.noContent().build();
    }
}
