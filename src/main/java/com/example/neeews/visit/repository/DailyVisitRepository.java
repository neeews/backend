package com.example.neeews.visit.repository;

import com.example.neeews.visit.domain.DailyVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyVisitRepository extends JpaRepository<DailyVisit, Long> {

    // JPQL엔 upsert가 없다. 조회 후 저장하면 동시에 들어온 핑끼리 증가분을 덮어써서 한 문장으로 올린다.
    @Modifying
    @Query(value = "INSERT INTO daily_visits (visit_date, visitor_count) VALUES (:date, 1) " +
                   "ON DUPLICATE KEY UPDATE visitor_count = visitor_count + 1",
           nativeQuery = true)
    void increment(@Param("date") LocalDate date);

    Optional<DailyVisit> findByVisitDate(LocalDate visitDate);

    @Query("SELECT COALESCE(SUM(v.visitorCount), 0L) FROM DailyVisit v")
    long sumVisitorCount();
}
