package com.liatrio.dora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "github_releases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "tag_name")
    private String tagName;

    @Column(name = "name")
    private String name;

    @Column(name = "prerelease", nullable = false)
    private boolean prerelease;

    @Column(name = "draft", nullable = false)
    private boolean draft;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
