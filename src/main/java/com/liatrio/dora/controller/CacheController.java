package com.liatrio.dora.controller;

import com.liatrio.dora.service.GitHubCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    private final GitHubCacheService cacheService;

    public CacheController(GitHubCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Void> invalidateCache(
            @PathVariable String owner,
            @PathVariable String repo) {
        log.info("Manual cache invalidation requested for {}/{}", owner, repo);
        cacheService.invalidate(owner, repo);
        return ResponseEntity.noContent().build();
    }
}
