package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubDeployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GithubDeploymentRepository extends JpaRepository<GithubDeployment, Long> {

    List<GithubDeployment> findByRepoIdAndCreatedAtBetween(String repoId, Instant start, Instant end);

    void deleteByRepoId(String repoId);
}
