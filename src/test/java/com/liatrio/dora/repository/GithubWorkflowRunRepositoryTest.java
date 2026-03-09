package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubWorkflowRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class GithubWorkflowRunRepositoryTest {

    @Autowired
    private GithubWorkflowRunRepository repository;

    @Test
    void savesAndFindsWorkflowRunByRepoIdBranchAndTimeRange() {
        Instant now = Instant.now();
        GithubWorkflowRun run = GithubWorkflowRun.builder()
                .githubId(2001L)
                .repoId("liatrio/test-repo")
                .name("CI")
                .status("completed")
                .conclusion("success")
                .headBranch("main")
                .createdAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        repository.save(run);

        List<GithubWorkflowRun> results = repository.findByRepoIdAndHeadBranchAndCreatedAtBetween(
                "liatrio/test-repo", "main",
                now.minus(2, ChronoUnit.HOURS), now);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getConclusion()).isEqualTo("success");
    }

    @Test
    void excludesRunsOnOtherBranches() {
        Instant now = Instant.now();
        GithubWorkflowRun featureRun = GithubWorkflowRun.builder()
                .githubId(2002L)
                .repoId("liatrio/test-repo")
                .name("CI")
                .status("completed")
                .conclusion("success")
                .headBranch("feature/my-feature")
                .createdAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        repository.save(featureRun);

        List<GithubWorkflowRun> results = repository.findByRepoIdAndHeadBranchAndCreatedAtBetween(
                "liatrio/test-repo", "main",
                now.minus(2, ChronoUnit.HOURS), now);

        assertThat(results).isEmpty();
    }
}
