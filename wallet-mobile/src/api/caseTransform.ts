/**
 * Backend (wallet-core) trả JSON snake_case (đã chuẩn hoá 1:1 sau khi thêm @JsonNaming vào
 * các DTO còn thiếu — xem `docs/api-endpoints-thuc-te-tren-dev-14-09.md`). Toàn bộ code FE
 * dùng camelCase (chuẩn TypeScript) — hàm này áp dụng 1 lần duy nhất trong response interceptor
 * của `client.ts`, để mọi hook/screen phía sau chỉ thấy camelCase, không tự chuyển đổi rải rác.
 *
 * Không dùng cho request (payload gửi lên vẫn phải snake_case tay theo đúng field JSON backend
 * đọc, ví dụ `idempotency_key`, `full_name` — xem type Request tương ứng).
 */
function snakeToCamelKey(key: string): string {
  return key.replace(/_([a-z0-9])/g, (_match, char: string) => char.toUpperCase());
}

export function toCamelCase<T = unknown>(value: unknown): T {
  if (Array.isArray(value)) {
    return value.map((item) => toCamelCase(item)) as unknown as T;
  }

  if (value !== null && typeof value === 'object' && !(value instanceof Date)) {
    const result: Record<string, unknown> = {};
    for (const [key, val] of Object.entries(value as Record<string, unknown>)) {
      result[snakeToCamelKey(key)] = toCamelCase(val);
    }
    return result as T;
  }

  return value as T;
}
