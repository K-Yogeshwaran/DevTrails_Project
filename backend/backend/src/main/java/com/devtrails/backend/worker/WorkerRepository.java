package com.devtrails.backend.worker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByWorkerId(String workerId);

    Optional<Worker> findByPhone(String phone);

    Optional<Worker> findByEmail(String email);

    List<Worker> findByZoneIdAndIsActive(String zoneId, Boolean isActive);

    List<Worker> findByIsActive(Boolean isActive);

    @Query("SELECT w.zoneId, COUNT(w) FROM Worker w WHERE w.isActive = true GROUP BY w.zoneId")
    List<Object[]> countActiveWorkersByZone();

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByWorkerId(String workerId);
}