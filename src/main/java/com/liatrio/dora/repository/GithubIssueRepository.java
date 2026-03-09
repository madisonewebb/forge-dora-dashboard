package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GithubIssueRepository extends JpaRepository<GithubIssue, Long> {

    List<GithubIssue> findByRepoIdAndCreatedAtBetween(String repoId, Instant start, Instant end);

    void deleteByRepoId(String repoId);
}
