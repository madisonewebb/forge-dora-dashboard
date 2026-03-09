package com.liatrio.dora.repository;

import com.liatrio.dora.model.GithubCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubCacheEntryRepository extends JpaRepository<GithubCacheEntry, Long> {

    Optional<GithubCacheEntry> findByRepoIdAndDataType(String repoId, String dataType);

    void deleteByRepoId(String repoId);
}
