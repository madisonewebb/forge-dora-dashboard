package com.liatrio.dora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "github_pull_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubPullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "title")
    private String title;

    /** Comma-delimited label names, e.g. "hotfix,bug" */
    @Column(name = "labels", length = 1000)
    private String labels;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "first_commit_at")
    private Instant firstCommitAt;

    @Column(name = "merge_commit_sha")
    private String mergeCommitSha;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
