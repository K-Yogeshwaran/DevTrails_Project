package com.devtrails.backend.claimlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClaimProcessingLogRepository extends JpaRepository<ClaimProcessingLog, Long> {

    List<ClaimProcessingLog> findByClaimIdOrderByCreatedAtAsc(String claimId);

    // Find all logs for claims belonging to a worker — used to find pending claims by eventId
    List<ClaimProcessingLog> findByClaimIdIn(List<String> claimIds);
}
