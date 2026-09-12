export function format_message_time_jst(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("ja-JP", {
    timeZone: "Asia/Tokyo", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(date);
}

export function format_created_at_jst(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("ja-JP", {
      timeZone: "Asia/Tokyo",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(date)
      .map(({ type, value }) => [type, value])
  );

  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}`;
}
