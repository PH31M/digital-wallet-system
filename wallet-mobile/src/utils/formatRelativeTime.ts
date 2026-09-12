function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

function formatDDMMYYYY(date: Date): string {
  return `${pad2(date.getDate())}/${pad2(date.getMonth() + 1)}/${date.getFullYear()}`;
}

export function formatRelativeTime(isoDate: string, now: Date = new Date()): string {
  const date = new Date(isoDate);
  const time = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;

  if (isSameDay(date, now)) return `Hôm nay, ${time}`;

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (isSameDay(date, yesterday)) return `Hôm qua, ${time}`;

  return formatDDMMYYYY(date);
}

/** Dùng cho tiêu đề nhóm theo ngày trong danh sách (vd. "Hôm nay, 10/09/2026"). */
export function formatDateGroupLabel(isoDate: string, now: Date = new Date()): string {
  const date = new Date(isoDate);
  const dateLabel = formatDDMMYYYY(date);

  if (isSameDay(date, now)) return `Hôm nay, ${dateLabel}`;

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (isSameDay(date, yesterday)) return `Hôm qua, ${dateLabel}`;

  return dateLabel;
}

/** Dùng cho timestamp giờ:phút riêng trong 1 dòng giao dịch (vd. "14:32"). */
export function formatTimeOfDay(isoDate: string): string {
  const date = new Date(isoDate);
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

/** Khoá nhóm theo ngày (yyyy-mm-dd theo local time) để gom giao dịch cùng ngày lại với nhau. */
export function dateGroupKey(isoDate: string): string {
  const date = new Date(isoDate);
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}
