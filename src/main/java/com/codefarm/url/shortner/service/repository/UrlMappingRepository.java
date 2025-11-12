package com.codefarm.url.shortner.service.repository;

import com.codefarm.url.shortner.service.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {
    boolean existsByShortCode(String shortCode);
    Optional<UrlMapping> findByLongUrl(String longUrl);

    interface UserUrlCount {
        String getUserId();
        long getCount();
    }

    @Query("select m.userId as userId, count(m) as count from UrlMapping m where m.userId is not null group by m.userId")
    List<UserUrlCount> countUrlsPerUser();
}


