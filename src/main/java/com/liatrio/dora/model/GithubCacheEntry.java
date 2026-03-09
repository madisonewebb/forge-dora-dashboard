package com.liatrio.dora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "github_cache_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "ttl_minutes", nullable = false)
    private int ttlMinutes;

    public boolean isExpired() {
        return Instant.now().isAfter(fetchedAt.plusSeconds(ttlMinutes * 60L));
    }
}
