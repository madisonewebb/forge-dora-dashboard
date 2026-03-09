package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubPullRequest;
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
class GithubPullRequestRepositoryTest {

    @Autowired
    private GithubPullRequestRepository repository;

    @Test
    void savesAndFindsMergedPrByRepoIdAndMergedAtRange() {
        Instant now = Instant.now();
        GithubPullRequest pr = GithubPullRequest.builder()
                .githubId(3001L)
                .repoId("liatrio/test-repo")
                .title("Add feature X")
                .labels("enhancement")
                .mergedAt(now.minus(2, ChronoUnit.HOURS))
                .createdAt(now.minus(1, ChronoUnit.DAYS))
                .build();
        repository.save(pr);

        List<GithubPullRequest> results = repository.findByRepoIdAndMergedAtBetween(
                "liatrio/test-repo",
                now.minus(30, ChronoUnit.DAYS),
                now);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Add feature X");
    }

    @Test
    void excludesUnmergedPrs() {
        Instant now = Instant.now();
        GithubPullRequest openPr = GithubPullRequest.builder()
                .githubId(3002L)
                .repoId("liatrio/test-repo")
                .title("WIP: in progress")
                .mergedAt(null)
                .createdAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        repository.save(openPr);

        List<GithubPullRequest> results = repository.findByRepoIdAndMergedAtBetween(
                "liatrio/test-repo",
                now.minus(30, ChronoUnit.DAYS),
                now);

        assertThat(results).isEmpty();
    }
}
