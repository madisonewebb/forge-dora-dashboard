package com.liatrio.dora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "github_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "title")
    private String title;

    @Column(name = "state")
    private String state;

    /** Comma-delimited label names, e.g. "incident,production" */
    @Column(name = "labels", length = 1000)
    private String labels;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
