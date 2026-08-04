export type RoomCategory = {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
};

export type UserSummary = {
  id: number;
  name: string;
  iconUrl: string | null;
};

export type RoomSummary = {
  id: number;
  name: string;
  category: RoomCategory;
  workStyle: "FOCUS" | "CHAT_OK";
  maxMembers: number;
  currentMembers: number;
  visibility: "PUBLIC" | "INVITE_ONLY" | "FRIENDS_ONLY";
  status: "OPEN" | "CLOSED";
  createdBy: UserSummary;
  joinable: boolean;
  joinRestriction: string | null;
  createdAt: string;
};

export type WorkSession = {
  id: number;
  room: RoomSummary;
  category: RoomCategory;
  participants: UserSummary[];
  startedAt: string;
  endedAt: string | null;
  durationSeconds: number;
};

export type PageMeta = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type WorkSessionPage = {
  items: WorkSession[];
  page: PageMeta;
};

export type WorkSessionSummary = {
  totalDurationSeconds: number;
  byCategory: Array<{
    category: RoomCategory;
    durationSeconds: number;
  }>;
  byDate: Array<{
    date: string;
    durationSeconds: number;
  }>;
};

export type WorkHistoryFilters = {
  from?: string;
  to?: string;
  categoryId?: number;
  page?: number;
  size?: number;
};

function queryString(filters: WorkHistoryFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      params.set(key, String(value));
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { Accept: "application/json" },
    signal,
  });
  if (!response.ok) {
    throw new Error(
      response.status === 401
        ? "ログインの有効期限が切れました。もう一度ログインしてください。"
        : "作業履歴を取得できませんでした。時間をおいて再度お試しください。",
    );
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    throw new Error("ログイン状態を確認できませんでした。もう一度ログインしてください。");
  }
  return response.json() as Promise<T>;
}

export function fetchWorkSessions(filters: WorkHistoryFilters, signal?: AbortSignal) {
  return getJson<WorkSessionPage>(
    `/api/v1/work-sessions${queryString(filters)}`,
    signal,
  );
}

export function fetchWorkSessionSummary(
  filters: Pick<WorkHistoryFilters, "from" | "to">,
  signal?: AbortSignal,
) {
  return getJson<WorkSessionSummary>(
    `/api/v1/work-sessions/summary${queryString(filters)}`,
    signal,
  );
}
