package com.example.neeews.huggingface.repository;

import com.example.neeews.huggingface.domain.ModelRelease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ModelReleaseRepository extends JpaRepository<ModelRelease, Long> {

    List<ModelRelease> findByModelIdIn(Collection<String> modelIds);

    Page<ModelRelease> findByLikesGreaterThanEqualOrderByReleasedAtDesc(long minLikes, Pageable pageable);

    List<ModelRelease> findByLikesGreaterThanEqualAndAiSummaryIsNullAndReleasedAtAfter(
            long minLikes, LocalDateTime releasedAfter, Pageable pageable);

    List<ModelRelease> findByVisibleAtAfterOrderByLikesDesc(LocalDateTime visibleAfter);
}
