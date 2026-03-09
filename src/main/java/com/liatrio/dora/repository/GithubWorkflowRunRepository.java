package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubWorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GithubWorkflowRunRepository extends JpaRepository<GithubWorkflowRun, Long> {

    List<GithubWorkflowRun> findByRepoIdAndHeadBranchAndCreatedAtBetween(
            String repoId, String headBranch, Instant start, Instant end);

    void deleteByRepoId(String repoId);
}
