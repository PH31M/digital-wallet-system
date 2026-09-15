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
