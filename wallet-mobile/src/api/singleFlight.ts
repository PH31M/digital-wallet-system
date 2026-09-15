/**
 * Đảm bảo nhiều lệnh gọi đồng thời chỉ thực thi `factory` đúng 1 lần, mọi lệnh gọi khác chờ
 * chung kết quả — dùng để gộp N request 401 xảy ra cùng lúc thành đúng 1 lần gọi
 * `POST /auth/refresh` (xem `client.ts`), tránh 2 refresh token cùng lúc làm invalidate lẫn
 * nhau ở backend.
 *
 * An toàn vì JS đơn luồng: giữa lúc đọc `inFlight` (còn null hay không) và lúc gán `inFlight =
 * factory()...` KHÔNG có `await` nào — nghĩa là đoạn code này chạy trọn vẹn, không nhường
 * quyền điều khiển cho microtask khác xen vào giữa chừng. Lệnh gọi đồng thời đầu tiên luôn
 * chạy xong dòng gán này trước khi lệnh gọi thứ 2 được xử lý, nên lệnh gọi thứ 2 trở đi luôn
 * thấy `inFlight` đã có sẵn và chỉ `return inFlight` — không gọi lại `factory`.
 */
export function createSingleFlight<T>(factory: () => Promise<T>): () => Promise<T> {
  let inFlight: Promise<T> | null = null;

  return () => {
    inFlight ??= factory().finally(() => {
      inFlight = null;
    });
    return inFlight;
  };
}
