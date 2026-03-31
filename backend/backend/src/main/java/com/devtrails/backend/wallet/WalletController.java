package com.devtrails.backend.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    // GET /api/wallet/{workerId}
    @GetMapping("/{workerId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable String workerId) {
        return ResponseEntity.ok(walletService.getWallet(workerId));
    }

    // GET /api/wallet/{workerId}/transactions
    @GetMapping("/{workerId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactions(
            @PathVariable String workerId) {
        return ResponseEntity.ok(walletService.getTransactions(workerId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "wallet-service"));
    }
}
