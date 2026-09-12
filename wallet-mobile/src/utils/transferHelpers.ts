/**
 * Bảng "số tiền bằng chữ" chỉ có sẵn cho các mốc preset trong code.html gốc (Chuyển tiền) —
 * bản gốc cũng KHÔNG tự tách chữ số thật cho số tiền bất kỳ, chỉ fallback về
 * "<số> đồng". Port đúng hành vi đó, không tự chế thêm thuật toán đọc số tiếng Việt đầy đủ.
 */
const KNOWN_AMOUNT_WORDS: Record<number, string> = {
  100_000: 'Một trăm nghìn đồng',
  200_000: 'Hai trăm nghìn đồng',
  500_000: 'Năm trăm nghìn đồng',
  1_000_000: 'Một triệu đồng chẵn',
  1_200_000: 'Một triệu hai trăm nghìn đồng',
  2_000_000: 'Hai triệu đồng chẵn',
};

export function getAmountInWords(amount: number): string {
  return KNOWN_AMOUNT_WORDS[amount] ?? `${amount.toLocaleString('vi-VN')} đồng`;
}

export function generateTransactionCode(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const d = date.getDate().toString().padStart(2, '0');
  const random = Math.floor(100000 + Math.random() * 900000);
  return `TXN-${y}${m}${d}-${random}`;
}

import { TransactionResultVariant, TransferDraft } from '../navigation/RootNavigator';

/** Dựng tham số Kết quả giao dịch cho luồng Chuyển tiền từ dữ liệu đã thu thập ở TransferScreen. */
export function buildTransferResultParams(
  draft: TransferDraft,
  variant: TransactionResultVariant,
  balanceAfter: number,
) {
  return {
    kind: 'transfer' as const,
    variant,
    amount: draft.amount,
    counterpartyLabel: 'Người nhận',
    counterpartyName: draft.recipientName,
    counterpartyDetail: `${draft.recipientWalletCode} (${draft.recipientWalletLabel})`,
    sourceLabel: 'Nguồn tiền',
    sourceDetail: 'Ví chính (•••• 8829)',
    note: draft.note,
    transactionCode: generateTransactionCode(),
    balanceAfter,
  };
}
