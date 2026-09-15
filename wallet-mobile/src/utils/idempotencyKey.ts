/**
 * `idempotency_key` không bắt buộc ở tầng validate backend (không có @NotNull/@NotBlank trên
 * `WalletAmountRequest`/`TransferRequest`) nhưng bắt buộc về nghiệp vụ — thiếu key thì
 * `WalletService.normalizeIdempotencyKey` coi là null, bỏ qua kiểm tra trùng lặp, mất khả năng
 * chống double-charge khi client tự động retry (mất mạng, timeout...). Không cần định dạng
 * UUID chuẩn (field chỉ là String tự do), nên không cần thêm dependency `uuid`/`expo-crypto`.
 */
export function generateIdempotencyKey(): string {
  const random = Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
  return `idem-${Date.now().toString(36)}-${random}`;
}
