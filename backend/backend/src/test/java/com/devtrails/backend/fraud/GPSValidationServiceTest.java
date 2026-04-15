package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GPSValidationServiceTest {

    private GPSValidationService gpsValidationService;
    private Claim testClaim;
    private Worker testWorker;
    private List<Claim> recentClaims;

    @BeforeEach
    void setUp() {
        gpsValidationService = new GPSValidationService();
        testClaim = createTestClaim();
        testWorker = createTestWorker();
        recentClaims = Arrays.asList(createRecentClaim(1), createRecentClaim(2));
    }

    @Test
    void testValidateGPS_ValidCoordinates() {
        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertTrue(result.isValid());
        assertTrue(result.getRiskScore() < 0.5);
        assertNotNull(result.getDetails());
    }

    @Test
    void testValidateGPS_MissingCoordinates() {
        testClaim.setGpsLatitude(null);
        testClaim.setGpsLongitude(null);

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertEquals(0.6, result.getRiskScore());
        assertTrue(result.getDetails().contains("GPS coordinates missing"));
    }

    @Test
    void testValidateGPS_OutsideZone() {
        // Set coordinates outside zone-001
        testClaim.setGpsLatitude(new BigDecimal("13.5000"));
        testClaim.setGpsLongitude(new BigDecimal("78.5000"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.7);
        assertTrue(result.getDetails().contains("outside declared zone"));
    }

    @Test
    void testValidateGPS_ImpossibleMovement() {
        // Create a claim with impossible movement
        Claim impossibleClaim = createTestClaim();
        impossibleClaim.setGpsLatitude(new BigDecimal("20.0000")); // Far away
        impossibleClaim.setGpsLongitude(new BigDecimal("80.0000"));
        impossibleClaim.setCreatedAt(LocalDateTime.now().minusMinutes(30)); // Only 30 minutes ago

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(impossibleClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.8);
        assertTrue(result.getDetails().contains("Impossible movement"));
    }

    @Test
    void testValidateGPS_TeleportationDetected() {
        // Create a claim with teleportation (same time, different location)
        Claim teleportClaim = createTestClaim();
        teleportClaim.setGpsLatitude(new BigDecimal("13.0000"));
        teleportClaim.setGpsLongitude(new BigDecimal("78.0000"));
        teleportClaim.setCreatedAt(recentClaims.get(0).getCreatedAt()); // Same time as last claim

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(teleportClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertEquals(0.9, result.getRiskScore());
        assertTrue(result.getDetails().contains("Teleportation detected"));
    }

    @Test
    void testValidateGPS_RoundNumberPattern() {
        // Test with coordinates that have too many round numbers
        testClaim.setGpsLatitude(new BigDecimal("12.000000"));
        testClaim.setGpsLongitude(new BigDecimal("77.000000"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_CoordinateAtNullIsland() {
        // Test with coordinates at 0,0 (Null Island)
        testClaim.setGpsLatitude(new BigDecimal("0.000000"));
        testClaim.setGpsLongitude(new BigDecimal("0.000000"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_UnrealisticPrecision() {
        // Test with coordinates that have too much precision
        testClaim.setGpsLatitude(new BigDecimal("12.971600000000001"));
        testClaim.setGpsLongitude(new BigDecimal("77.594600000000001"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_CoordinatesAtCardinalPoints() {
        // Test with coordinates at exact cardinal points
        testClaim.setGpsLatitude(new BigDecimal("13.000000"));
        testClaim.setGpsLongitude(new BigDecimal("77.594600"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_LatEqualsLonPattern() {
        // Test with latitude approximately equal to longitude
        testClaim.setGpsLatitude(new BigDecimal("12.971600"));
        testClaim.setGpsLongitude(new BigDecimal("12.971601"));

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_InvalidCoordinates() {
        // Test with invalid coordinates (out of range)
        testClaim.setGpsLatitude(new BigDecimal("95.000000")); // Invalid latitude
        testClaim.setGpsLongitude(new BigDecimal("200.000000")); // Invalid longitude

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.9);
        assertTrue(result.getDetails().contains("spoofing patterns"));
    }

    @Test
    void testValidateGPS_InconsistentWorkerPattern() {
        // Test with zone mismatch
        testWorker.setZoneId("zone-002"); // Different from claim zone

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, recentClaims);

        assertTrue(result.isValid()); // Still valid but with increased risk
        assertTrue(result.getRiskScore() > 0.0);
    }

    @Test
    void testValidateGPS_NoRecentClaims() {
        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, Collections.emptyList());

        assertTrue(result.isValid());
        assertTrue(result.getRiskScore() < 0.5);
    }

    @Test
    void testValidateGPS_RecentClaimsWithoutGPS() {
        // Create recent claims without GPS data
        List<Claim> claimsWithoutGPS = Arrays.asList(
            createClaimWithoutGPS(1),
            createClaimWithoutGPS(2)
        );

        GPSValidationService.GPSValidationResult result = 
            gpsValidationService.validateGPS(testClaim, testWorker, claimsWithoutGPS);

        assertTrue(result.isValid());
        assertTrue(result.getRiskScore() < 0.5);
    }

    private Claim createTestClaim() {
        Claim claim = new Claim();
        claim.setClaimId("CLM-TEST001-1234567890");
        claim.setWorkerId("WORKER001");
        claim.setZoneId("zone-001");
        claim.setGpsLatitude(new BigDecimal("12.971600"));
        claim.setGpsLongitude(new BigDecimal("77.594600"));
        claim.setCreatedAt(LocalDateTime.now());
        claim.setTriggeredAt(LocalDateTime.now());
        return claim;
    }

    private Claim createRecentClaim(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-TEST00" + hoursAgo + "-1234567890");
        claim.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setGpsLatitude(new BigDecimal("12.971600").add(new BigDecimal(hoursAgo * 0.01)));
        claim.setGpsLongitude(new BigDecimal("77.594600").add(new BigDecimal(hoursAgo * 0.01)));
        return claim;
    }

    private Claim createClaimWithoutGPS(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-NOGPS00" + hoursAgo + "-1234567890");
        claim.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setGpsLatitude(null);
        claim.setGpsLongitude(null);
        return claim;
    }

    private Worker createTestWorker() {
        Worker worker = new Worker();
        worker.setWorkerId("WORKER001");
        worker.setZoneId("zone-001");
        return worker;
    }
}
