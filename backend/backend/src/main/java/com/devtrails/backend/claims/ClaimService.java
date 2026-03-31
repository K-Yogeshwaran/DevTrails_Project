package com.devtrails.backend.claims;

import com.devtrails.backend.config.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;

    public List<ClaimDTO.ClaimSummary> getWorkerClaims(String workerId) {
        return claimRepository
                .findByWorkerIdOrderByCreatedAtDesc(workerId)
                .stream()
                .map(c -> new ClaimDTO.ClaimSummary(
                        c.getClaimId(), c.getTriggerType(),
                        c.getPayoutAmount(), c.getStatus(), c.getTriggeredAt()
                ))
                .collect(Collectors.toList());
    }

    public ClaimDTO.ClaimResponse getClaimById(String claimId) {
        Claim c = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Claim not found with ID: " + claimId));
        return buildResponse(c, "Claim details");
    }

    public ClaimDTO.Analytics getAnalytics() {
        return getAnalyticsForWorker(null);
    }

    public ClaimDTO.Analytics getAnalyticsForWorker(String workerId) {
        List<Claim> all = workerId != null
                ? claimRepository.findByWorkerIdOrderByCreatedAtDesc(workerId)
                : claimRepository.findAll();
        long total           = all.size();
        long approved        = all.stream().filter(c -> "approved".equals(c.getStatus())).count();
        long rejected        = all.stream().filter(c -> "rejected".equals(c.getStatus())).count();
        long flagged         = all.stream().filter(c -> "flagged".equals(c.getStatus())).count();
        BigDecimal totalPaid = all.stream()
                .filter(c -> "approved".equals(c.getStatus()))
                .map(Claim::getPayoutAmount)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;
        double fraudRate    = total > 0 ? (double) flagged  / total * 100 : 0;

        return new ClaimDTO.Analytics(total, approved, rejected, flagged,
                totalPaid, approvalRate, fraudRate);
    }

    private ClaimDTO.ClaimResponse buildResponse(Claim c, String message) {
        return new ClaimDTO.ClaimResponse(
                c.getClaimId(), c.getWorkerId(), c.getPolicyNumber(),
                c.getTriggerType(), c.getTriggerValue(), c.getZoneId(),
                c.getDisruptedHours(), c.getPayoutAmount(), c.getFraudScore(),
                c.getStatus(), c.getTriggeredAt(), c.getProcessedAt(), message
        );
    }
}
