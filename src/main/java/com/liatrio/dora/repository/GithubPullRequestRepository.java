package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GithubPullRequestRepository extends JpaRepository<GithubPullRequest, Long> {

    List<GithubPullRequest> findByRepoIdAndMergedAtBetween(String repoId, Instant start, Instant end);

    void deleteByRepoId(String repoId);
}
