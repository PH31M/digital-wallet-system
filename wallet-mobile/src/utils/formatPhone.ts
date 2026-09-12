/** "0987654321" -> "+84 98***4321" (che 3 số giữa, giữ 2 số đầu + 4 số cuối). */
export function maskPhoneNumber(phone: string): string {
  const digits = phone.replace(/\D/g, '').replace(/^0/, '');
  if (digits.length < 6) return `+84 ${digits}`;
  const first = digits.slice(0, 2);
  const last = digits.slice(-4);
  return `+84 ${first}***${last}`;
}
