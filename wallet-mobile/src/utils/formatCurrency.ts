export function formatCurrency(amount: number): string {
  const rounded = Math.round(Math.abs(amount));
  return `${rounded.toLocaleString('vi-VN')} đ`;
}
