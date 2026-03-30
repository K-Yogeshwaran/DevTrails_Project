package com.devtrails.backend.policy;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {


    @Query("SELECT p FROM Policy p WHERE p.workerId = :workerId " +
            "AND p.status = 'active' " +
            "AND :date BETWEEN p.weekStart AND p.weekEnd")
    Optional<Policy> findActivePolicy(
            @Param("workerId") String workerId,
            @Param("date") LocalDate date
    );

    List<Policy> findByWorkerIdOrderByCreatedAtDesc(String workerId);


    Optional<Policy> findByPolicyNumber(String policyNumber);


    @Query("SELECT COUNT(p) > 0 FROM Policy p WHERE p.workerId = :workerId " +
            "AND p.status = 'active' " +
            "AND :weekStart = p.weekStart")
    boolean existsActivePolicyForWeek(
            @Param("workerId") String workerId,
            @Param("weekStart") LocalDate weekStart
    );

    List<Policy> findByStatus(String status);

    @Query("SELECT p.tier, COUNT(p) FROM Policy p " +
            "WHERE p.status = 'active' GROUP BY p.tier")
    List<Object[]> countActivePoliciesByTier();
}