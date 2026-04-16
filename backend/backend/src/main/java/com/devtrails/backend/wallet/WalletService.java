package com.devtrails.backend.wallet;

import com.devtrails.backend.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository            walletRepo;
    private final WalletTransactionRepository txRepo;

    public WalletService(WalletRepository walletRepo, WalletTransactionRepository txRepo) {
        this.walletRepo = walletRepo;
        this.txRepo = txRepo;
    }

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    // ── CREATE WALLET (called on worker registration) ─────────
    @Transactional
    public Wallet createWallet(String workerId) {
        if (walletRepo.existsByWorkerId(workerId)) {
            return walletRepo.findByWorkerId(workerId).get();
        }

        Wallet wallet = new Wallet();
        wallet.setWorkerId(workerId);
        wallet.setBalance(INITIAL_BALANCE);
        wallet.setTotalCredited(INITIAL_BALANCE);
        wallet.setTotalDebited(BigDecimal.ZERO);
        walletRepo.save(wallet);

        // Record the initial credit transaction
        WalletTransaction tx = WalletTransaction.credit(
                workerId, INITIAL_BALANCE, INITIAL_BALANCE,
                "Welcome bonus — GigShield test wallet loaded",
                "INIT-" + workerId,
                "initial_credit"
        );
        txRepo.save(tx);

        log.info("Wallet created for worker {} with ₹{} balance", workerId, INITIAL_BALANCE);
        return wallet;
    }

    // ── DEBIT (premium collection) ────────────────────────────
    @Transactional
    public WalletTransaction debit(String workerId, BigDecimal amount,
                                   String description, String referenceId, String category) {
        Wallet wallet = getWallet(workerId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Insufficient wallet balance. Available: ₹" + wallet.getBalance()
                    + " | Required: ₹" + amount);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setTotalDebited(wallet.getTotalDebited().add(amount));
        walletRepo.save(wallet);

        WalletTransaction tx = WalletTransaction.debit(
                workerId, amount, newBalance, description, referenceId, category
        );
        txRepo.save(tx);

        log.info("Wallet debit: worker={} amount=₹{} balance=₹{} ref={}",
                workerId, amount, newBalance, referenceId);
        return tx;
    }

    // ── CREDIT (claim payout) ─────────────────────────────────
    @Transactional
    public WalletTransaction credit(String workerId, BigDecimal amount,
                                    String description, String referenceId, String category) {
        Wallet wallet = getWallet(workerId);

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet.setTotalCredited(wallet.getTotalCredited().add(amount));
        walletRepo.save(wallet);

        WalletTransaction tx = WalletTransaction.credit(
                workerId, amount, newBalance, description, referenceId, category
        );
        txRepo.save(tx);

        log.info("Wallet credit: worker={} amount=₹{} balance=₹{} ref={}",
                workerId, amount, newBalance, referenceId);
        return tx;
    }

    // ── GET WALLET ────────────────────────────────────────────
    public Wallet getWallet(String workerId) {
        return walletRepo.findByWorkerId(workerId)
                .orElseGet(() -> createWallet(workerId));
    }

    // ── GET TRANSACTIONS ──────────────────────────────────────
    public List<WalletTransaction> getTransactions(String workerId) {
        return txRepo.findByWorkerIdOrderByCreatedAtDesc(workerId);
    }

    // ── CHECK BALANCE ─────────────────────────────────────────
    public boolean hasSufficientBalance(String workerId, BigDecimal amount) {
        Wallet wallet = getWallet(workerId);
        return wallet.getBalance().compareTo(amount) >= 0;
    }
}
