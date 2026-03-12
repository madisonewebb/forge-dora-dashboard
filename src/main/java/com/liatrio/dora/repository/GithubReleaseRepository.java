package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GithubReleaseRepository extends JpaRepository<GithubRelease, Long> {

    List<GithubRelease> findByRepoIdAndPublishedAtBetween(String repoId, Instant start, Instant end);

    void deleteByRepoId(String repoId);
}
