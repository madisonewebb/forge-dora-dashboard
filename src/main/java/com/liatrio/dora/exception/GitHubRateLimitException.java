package com.liatrio.dora.exception;

import java.time.Instant;

public class GitHubRateLimitException extends RuntimeException {

    private final Instant resetsAt;

    public GitHubRateLimitException(Instant resetsAt) {
        super("GitHub API rate limit exceeded. Resets at: " + resetsAt);
        this.resetsAt = resetsAt;
    }

    public Instant getResetsAt() {
        return resetsAt;
    }
}
