package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubDeployment;
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
class GithubDeploymentRepositoryTest {

    @Autowired
    private GithubDeploymentRepository repository;

    @Test
    void savesAndFindsDeploymentByRepoIdAndTimeRange() {
        Instant now = Instant.now();
        GithubDeployment deployment = GithubDeployment.builder()
                .githubId(1001L)
                .repoId("liatrio/test-repo")
                .environment("production")
                .status("success")
                .createdAt(now.minus(1, ChronoUnit.HOURS))
                .updatedAt(now)
                .build();
        repository.save(deployment);

        List<GithubDeployment> results = repository.findByRepoIdAndCreatedAtBetween(
                "liatrio/test-repo",
                now.minus(2, ChronoUnit.HOURS),
                now);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGithubId()).isEqualTo(1001L);
        assertThat(results.get(0).getStatus()).isEqualTo("success");
    }

    @Test
    void excludesDeploymentsOutsideTimeRange() {
        Instant now = Instant.now();
        GithubDeployment oldDeployment = GithubDeployment.builder()
                .githubId(1002L)
                .repoId("liatrio/test-repo")
                .environment("production")
                .status("success")
                .createdAt(now.minus(60, ChronoUnit.DAYS))
                .build();
        repository.save(oldDeployment);

        List<GithubDeployment> results = repository.findByRepoIdAndCreatedAtBetween(
                "liatrio/test-repo",
                now.minus(30, ChronoUnit.DAYS),
                now);

        assertThat(results).isEmpty();
    }
}
