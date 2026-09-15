import { createSingleFlight } from '../singleFlight';

describe('createSingleFlight', () => {
  it('gộp 5 lệnh gọi đồng thời (Promise.all) thành đúng 1 lần thực thi factory', async () => {
    let callCount = 0;
    const refresh = createSingleFlight(async () => {
      callCount += 1;
      // Mô phỏng độ trễ mạng thật của POST /auth/refresh — nếu logic gộp sai (mỗi lệnh gọi
      // tự chạy factory riêng), callCount sẽ ra 5 vì cả 5 đều fire trước khi cái đầu tiên
      // resolve xong.
      await new Promise((resolve) => setTimeout(resolve, 20));
      return 'new-access-token';
    });

    const results = await Promise.all([refresh(), refresh(), refresh(), refresh(), refresh()]);

    expect(callCount).toBe(1);
    expect(results).toEqual(Array(5).fill('new-access-token'));
  });

  it('cho phép gọi lại factory ở lần refresh KẾ TIẾP, sau khi lần trước đã xong', async () => {
    let callCount = 0;
    const refresh = createSingleFlight(async () => {
      callCount += 1;
      return callCount;
    });

    const first = await refresh();
    const second = await refresh();

    // Không được kẹt cứng ở lần gọi đầu tiên mãi mãi — token hết hạn lần 2 phải refresh lại thật.
    expect(first).toBe(1);
    expect(second).toBe(2);
    expect(callCount).toBe(2);
  });

  it('vẫn gộp đúng khi factory reject — không để lỗi làm rò rỉ state (inFlight không bị kẹt null mãi)', async () => {
    let callCount = 0;
    const refresh = createSingleFlight(async () => {
      callCount += 1;
      throw new Error('refresh token expired');
    });

    const calls = [refresh(), refresh(), refresh()];
    await expect(Promise.all(calls.map((p) => p.catch((e: Error) => e.message)))).resolves.toEqual([
      'refresh token expired',
      'refresh token expired',
      'refresh token expired',
    ]);
    expect(callCount).toBe(1);

    // Sau khi fail, lệnh gọi mới vẫn phải thử lại (không bị đóng băng ở trạng thái lỗi cũ).
    await expect(refresh().catch((e: Error) => e.message)).resolves.toBe('refresh token expired');
    expect(callCount).toBe(2);
  });
});
