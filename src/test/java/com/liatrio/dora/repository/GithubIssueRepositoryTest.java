package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubIssue;
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
class GithubIssueRepositoryTest {

    @Autowired
    private GithubIssueRepository repository;

    @Test
    void savesAndFindsIssueByRepoIdAndTimeRange() {
        Instant now = Instant.now();
        GithubIssue issue = GithubIssue.builder()
                .githubId(4001L)
                .repoId("liatrio/test-repo")
                .title("Production outage")
                .state("closed")
                .labels("incident,production")
                .createdAt(now.minus(3, ChronoUnit.HOURS))
                .closedAt(now.minus(1, ChronoUnit.HOURS))
                .build();
        repository.save(issue);

        List<GithubIssue> results = repository.findByRepoIdAndCreatedAtBetween(
                "liatrio/test-repo",
                now.minus(30, ChronoUnit.DAYS),
                now);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLabels()).contains("incident");
    }

    @Test
    void excludesIssuesOutsideTimeRange() {
        Instant now = Instant.now();
        GithubIssue oldIssue = GithubIssue.builder()
                .githubId(4002L)
                .repoId("liatrio/test-repo")
                .title("Old incident")
                .state("closed")
                .labels("incident")
                .createdAt(now.minus(90, ChronoUnit.DAYS))
                .build();
        repository.save(oldIssue);

        List<GithubIssue> results = repository.findByRepoIdAndCreatedAtBetween(
                "liatrio/test-repo",
                now.minus(30, ChronoUnit.DAYS),
                now);

        assertThat(results).isEmpty();
    }
}
