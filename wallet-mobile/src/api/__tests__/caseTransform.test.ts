import { toCamelCase } from '../caseTransform';

describe('toCamelCase', () => {
  it('chuyển key snake_case 1 tầng', () => {
    expect(toCamelCase({ full_name: 'An', phone_number: '090' })).toEqual({
      fullName: 'An',
      phoneNumber: '090',
    });
  });

  it('đệ quy vào object lồng nhiều tầng', () => {
    const input = {
      user_profile: {
        full_name: 'An',
        contact_info: { phone_number: '090', email_verified: true },
      },
    };
    expect(toCamelCase(input)).toEqual({
      userProfile: {
        fullName: 'An',
        contactInfo: { phoneNumber: '090', emailVerified: true },
      },
    });
  });

  it('đệ quy vào mảng object (vd content của PageResponse/Page)', () => {
    const input = {
      content: [
        { transaction_id: 't1', balance_after: 100 },
        { transaction_id: 't2', balance_after: 200 },
      ],
    };
    expect(toCamelCase(input)).toEqual({
      content: [
        { transactionId: 't1', balanceAfter: 100 },
        { transactionId: 't2', balanceAfter: 200 },
      ],
    });
  });

  it('giữ nguyên giá trị null/undefined thay vì crash (typeof null === "object")', () => {
    expect(toCamelCase(null)).toBeNull();
    expect(toCamelCase({ read_at: null, review_note: undefined })).toEqual({
      readAt: null,
      reviewNote: undefined,
    });
  });

  it('giữ nguyên primitive (string/number/boolean) không có key để đổi', () => {
    expect(toCamelCase('COMPLETED')).toBe('COMPLETED');
    expect(toCamelCase(42)).toBe(42);
    expect(toCamelCase(true)).toBe(true);
  });

  it('idempotent — key đã là camelCase thì chạy 1 hay 2 lần vẫn ra cùng kết quả (không đổi sai)', () => {
    const input = { fullName: 'An', isVerified: true, mfaRequired: false };
    const once = toCamelCase(input);
    const twice = toCamelCase(toCamelCase(input));
    expect(once).toEqual(input);
    expect(twice).toEqual(input);
  });

  it('KHÔNG động vào field/key đã camelCase sẵn của PageResponse (content/totalElements/...)', () => {
    const pageResponse = {
      content: [{ transaction_id: 't1' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
    };
    expect(toCamelCase(pageResponse)).toEqual({
      content: [{ transactionId: 't1' }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
      first: true,
      last: true,
    });
  });

  it('chuyển đúng key có chữ số ngay sau dấu gạch dưới (không xuất hiện trong 34 endpoint hiện tại, nhưng để biết giới hạn)', () => {
    expect(toCamelCase({ item_2_name: 'x' })).toEqual({ item2Name: 'x' });
  });

  it('GIỚI HẠN đã biết: 2 dấu gạch dưới liên tiếp không camelCase trọn vẹn (không có field nào trong API thật dùng kiểu này)', () => {
    // "a__b" -> chỉ dấu gạch dưới thứ 2 (ngay trước ký tự chữ) được khớp, dấu đầu bị bỏ sót.
    expect(toCamelCase({ a__b: 'x' })).toEqual({ a_B: 'x' });
  });

  it('không đệ quy vào Date instance (phòng thủ — axios/JSON thật không bao giờ trả Date, luôn là ISO string)', () => {
    const date = new Date('2026-09-14T00:00:00Z');
    expect(toCamelCase({ created_at: date })).toEqual({ createdAt: date });
  });

  it('mô phỏng đúng envelope ApiResponse thật của backend — ApiResponse.java KHÔNG có @JsonNaming, field requestId serialize thẳng ra "requestId" (camelCase có sẵn, không phải "request_id")', () => {
    const raw = {
      success: true,
      requestId: 'r-1',
      timestamp: '2026-09-14T00:00:00Z',
      data: { id: 'w-1', full_name: 'An', currency: 'VND', status: 'ACTIVE', balance: 1000 },
      error: null,
    };
    expect(toCamelCase(raw)).toEqual({
      success: true,
      requestId: 'r-1',
      timestamp: '2026-09-14T00:00:00Z',
      data: { id: 'w-1', fullName: 'An', currency: 'VND', status: 'ACTIVE', balance: 1000 },
      error: null,
    });
  });
});
