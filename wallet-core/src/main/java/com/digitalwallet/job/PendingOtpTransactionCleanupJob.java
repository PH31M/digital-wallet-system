package com.digitalwallet.job;

import com.digitalwallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DWS-153: tự động fail các giao dịch treo ở PENDING_OTP_CONFIRMATION quá lâu (user không
 * confirm OTP). TTL khớp với TTL của OTP trong Redis (OtpService.getOtpTtl()) nên không có
 * khoảng trống giữa lúc OTP hết hạn và lúc giao dịch bị đánh fail.
 */
@Component
public class PendingOtpTransactionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpTransactionCleanupJob.class);

    private final WalletService walletService;

    public PendingOtpTransactionCleanupJob(WalletService walletService) {
        this.walletService = walletService;
    }

    @Scheduled(fixedDelayString = "${wallet.otp.cleanup-interval-ms:60000}")
    public void expireStalePendingOtpTransactions() {
        int expiredCount = walletService.expirePendingOtpTransactions();
        if (expiredCount > 0) {
            log.info("Expired {} transaction(s) stuck in PENDING_OTP_CONFIRMATION", expiredCount);
        }
    }
}
