package com.versus.api.scraping.repo;

import com.versus.api.scraping.SpiderStatus;
import com.versus.api.scraping.domain.Spider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpiderRepository extends JpaRepository<Spider, UUID> {
    long countByStatus(SpiderStatus status);
}
