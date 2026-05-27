import type {
  User,
  LoginRequest,
  LoginResponse,
  ProfileUpdateRequest,
  Timesheet,
  Schedule,
  Document as AppDocument,
  AppNotification,
} from "../types";

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// token management
let authToken: string | null = localStorage.getItem("token");

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (token) {
    localStorage.setItem("token", token);
  } else {
    localStorage.removeItem("token");
  }
};

export const getAuthToken = () => authToken;

// API Client
const apiClient = {
  async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...(authToken && { Authorization: `Bearer ${authToken}` }),
      ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ message: "Eroare necunoscută" }));
      throw new Error(error.message || `HTTP Error ${response.status}`);
    }

    // ne ocupam de raspunsurile empty
    const text = await response.text();
    return text ? JSON.parse(text) : (null as unknown as T);
  },

  get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "GET" });
  },

  post<T>(endpoint: string, data?: object): Promise<T> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  },

  put<T>(endpoint: string, data: object): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  patch<T>(endpoint: string, data?: object): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    });
  },

  delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "DELETE" });
  },
};

// auth api
export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>(
      "/auth/login",
      credentials,
    );
    setAuthToken(response.token);
    return response;
  },

  logout: () => {
    setAuthToken(null);
  },

  getCurrentUser: (): Promise<User> => {
    return apiClient.get<User>("/auth/me");
  },

  updateProfile: (data: ProfileUpdateRequest): Promise<User> => {
    return apiClient.put<User>("/auth/profile", data);
  },
};

// users api
export const usersApi = {
  getAll: (params?: {
    role?: string;
    departmentId?: string;
    search?: string;
  }): Promise<User[]> => {
    const clean: Record<string, string> = {};
    if (params?.role) clean.role = params.role;
    if (params?.departmentId) clean.departmentId = params.departmentId;
    if (params?.search) clean.search = params.search;
    const query = new URLSearchParams(clean).toString();
    return apiClient.get<User[]>(`/users${query ? `?${query}` : ""}`);
  },

  getById: (id: string): Promise<User> => {
    return apiClient.get<User>(`/users/${id}`);
  },

  create: (data: Partial<User>): Promise<User> => {
    return apiClient.post<User>("/users", data);
  },

  update: (id: string, data: Partial<User>): Promise<User> => {
    return apiClient.put<User>(`/users/${id}`, data);
  },

  updateStatus: (id: string, status: string): Promise<User> => {
    return apiClient.patch<User>(`/users/${id}/status?status=${status}`);
  },

  delete: (id: string): Promise<void> => {
    return apiClient.delete<void>(`/users/${id}`);
  },
};

// schedules api
export const schedulesApi = {
  getMine: (day?: string): Promise<Schedule[]> => {
    return apiClient.get<Schedule[]>(`/schedules${day ? `?day=${day}` : ""}`);
  },

  create: (data: Partial<Schedule>): Promise<Schedule> => {
    return apiClient.post<Schedule>("/schedules", data);
  },

  update: (id: string, data: Partial<Schedule>): Promise<Schedule> => {
    return apiClient.put<Schedule>(`/schedules/${id}`, data);
  },

  delete: (id: string): Promise<void> => {
    return apiClient.delete<void>(`/schedules/${id}`);
  },

  deleteAll: (): Promise<void> => {
    return apiClient.delete<void>("/schedules/all");
  },

  importCsv: async (file: File): Promise<Schedule[]> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/schedules/import/csv`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
      body: formData,
    });

    if (!response.ok) throw new Error("Eroare la importul CSV");
    return response.json();
  },

  importExcel: async (file: File): Promise<Schedule[]> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/schedules/import/excel`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
      body: formData,
    });

    if (!response.ok) throw new Error("Eroare la importul Excel");
    return response.json();
  },
};

// timesheets api
export const timesheetsApi = {
  getMine: (): Promise<Timesheet[]> => {
    return apiClient.get<Timesheet[]>("/timesheets");
  },

  getByPeriod: (month: number, year: number): Promise<Timesheet> => {
    return apiClient.get<Timesheet>(`/timesheets/${month}/${year}`);
  },

  getOrCreate: (month: number, year: number): Promise<Timesheet> => {
    return apiClient.post<Timesheet>("/timesheets", { month, year });
  },

  addEntry: (timesheetId: string, entry: object): Promise<Timesheet> => {
    return apiClient.post<Timesheet>(
      `/timesheets/${timesheetId}/entries`,
      entry,
    );
  },

  deleteEntry: (timesheetId: string, entryId: string): Promise<Timesheet> => {
    return apiClient.delete<Timesheet>(
      `/timesheets/${timesheetId}/entries/${entryId}`,
    );
  },

  submit: (timesheetId: string): Promise<Timesheet> => {
    return apiClient.post<Timesheet>(`/timesheets/${timesheetId}/submit`);
  },
};

// documents api
export const documentsApi = {
  getMine: (): Promise<AppDocument[]> => {
    return apiClient.get<AppDocument[]>("/documents");
  },

  generate: (
    timesheetId: string,
    annexType: string,
  ): Promise<{ id: string; fileName: string }> => {
    return apiClient.post(
      `/documents/generate?timesheetId=${timesheetId}&annexType=${annexType}`,
    );
  },

  download: async (documentId: string): Promise<Blob> => {
    const response = await fetch(
      `${API_BASE_URL}/documents/${documentId}/download`,
      {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      },
    );
    if (!response.ok) throw new Error("Eroare la descărcare");
    return response.blob();
  },
};

// secretariat api
export const secretariatApi = {
  getTimesheets: (
    month: number,
    year: number,
    params?: { departmentId?: string; status?: string },
  ): Promise<Timesheet[]> => {
    const query = new URLSearchParams({
      month: month.toString(),
      year: year.toString(),
      ...params,
    }).toString();
    return apiClient.get<Timesheet[]>(`/secretariat/timesheets?${query}`);
  },

  getTimesheetStatus: (
    month: number,
    year: number,
  ): Promise<{
    total: number;
    draft: number;
    submitted: number;
    approved: number;
    missing: number;
  }> => {
    return apiClient.get(
      `/secretariat/timesheets/status?month=${month}&year=${year}`,
    );
  },

  getTimesheetDetails: (timesheetId: string): Promise<Timesheet> => {
    return apiClient.get<Timesheet>(`/secretariat/timesheets/${timesheetId}`);
  },

  approveTimesheet: (timesheetId: string): Promise<Timesheet> => {
    return apiClient.post<Timesheet>(
      `/secretariat/timesheets/${timesheetId}/approve`,
    );
  },

  mergeDocuments: async (
    month: number,
    year: number,
    departmentId?: string,
  ): Promise<Blob> => {
    let url = `${API_BASE_URL}/secretariat/documents/merge?month=${month}&year=${year}`;
    if (departmentId) url += `&departmentId=${departmentId}`;

    const response = await fetch(url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
    });
    if (!response.ok) throw new Error("Eroare la concatenare");
    return response.blob();
  },
};

// notifications api
export const notificationsApi = {
  getAll: (): Promise<AppNotification[]> => {
    return apiClient.get<AppNotification[]>("/notifications");
  },

  getUnreadCount: (): Promise<{ count: number }> => {
    return apiClient.get<{ count: number }>("/notifications/unread-count");
  },

  markAsRead: (id: string): Promise<AppNotification> => {
    return apiClient.patch<AppNotification>(`/notifications/${id}/read`);
  },

  markAllAsRead: (): Promise<void> => {
    return apiClient.patch<void>("/notifications/read-all");
  },
};

export default apiClient;
