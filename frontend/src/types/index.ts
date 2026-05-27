// enums
export enum UserRole {
  CADRU_DIDACTIC = "CADRU_DIDACTIC",
  SECRETARIAT = "SECRETARIAT",
  ADMIN = "ADMIN",
}

export enum UserStatus {
  ACTIVE = "ACTIVE",
  INACTIVE = "INACTIVE",
}

export enum DayOfWeek {
  LUNI = "LUNI",
  MARTI = "MARTI",
  MIERCURI = "MIERCURI",
  JOI = "JOI",
  VINERI = "VINERI",
  SAMBATA = "SAMBATA",
  DUMINICA = "DUMINICA",
}

export enum ActivityType {
  CURS = "CURS",
  SEMINAR = "SEMINAR",
  LABORATOR = "LABORATOR",
  PROIECT = "PROIECT",
}

export enum TimesheetStatus {
  DRAFT = "DRAFT",
  SUBMITTED = "SUBMITTED",
  APPROVED = "APPROVED",
}

export enum HourType {
  NORMA = "NORMA",
  PLATA_ORA = "PLATA_ORA",
}

export enum AnnexType {
  ANEXA_1 = "ANEXA_1",
  ANEXA_3 = "ANEXA_3",
}

// interfete
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  departmentId?: string;
  departmentName?: string;
  departmentCode?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Department {
  id: string;
  name: string;
  code: string;
  createdAt: string;
}

export interface Schedule {
  id: string;
  userId: string;
  dayOfWeek: DayOfWeek;
  dayOfWeekDisplay: string;
  timeSlot: string;
  startTime: string;
  endTime: string;
  discipline: string;
  room?: string;
  activityType: ActivityType;
  activityTypeDisplay: string;
  durationHours: number;
  createdAt: string;
  updatedAt: string;
}

export interface Timesheet {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  departmentName?: string;
  month: number;
  year: number;
  periodDisplay: string;
  status: TimesheetStatus;
  statusDisplay: string;
  totalNormaHours: number;
  totalPlataOraHours: number;
  totalHours: number;
  entries?: TimesheetEntry[];
  submittedAt?: string;
  createdAt: string;
  updatedAt: string;
  editable: boolean;
}

export interface TimesheetEntry {
  id: string;
  timesheetId: string;
  entryDate: string;
  dayOfWeek: string;
  timeSlot: string;
  startTime: string;
  endTime: string;
  hourType: HourType;
  hourTypeDisplay: string;
  hourTypeColor: string;
  activity?: string;
  durationHours: number;
  createdAt: string;
}

export interface Document {
  id: string;
  userId: string;
  timesheetId: string;
  annexType: AnnexType;
  filePath: string;
  fileName: string;
  generatedAt: string;
}

export interface Notification {
  id: string;
  userId: string;
  notificationType: string;
  subject: string;
  message: string;
  isRead: boolean;
  isSent: boolean;
  sentAt?: string;
  createdAt: string;
}

// Tipul folosit pentru notificările din API (structura returnată de NotificationController)
export interface AppNotification {
  id: string;
  type: string;
  typeDisplay: string;
  subject: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

// types auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
  email: string;
  currentPassword?: string;
  newPassword?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  departmentName?: string;
  departmentId?: string;
}

// types helper
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details?: Record<string, string>;
}

// zilele sapt helpers
export const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: DayOfWeek.LUNI, label: "Luni" },
  { value: DayOfWeek.MARTI, label: "Marți" },
  { value: DayOfWeek.MIERCURI, label: "Miercuri" },
  { value: DayOfWeek.JOI, label: "Joi" },
  { value: DayOfWeek.VINERI, label: "Vineri" },
  { value: DayOfWeek.SAMBATA, label: "Sâmbătă" },
  { value: DayOfWeek.DUMINICA, label: "Duminică" },
];

export const ACTIVITY_TYPES: { value: ActivityType; label: string }[] = [
  { value: ActivityType.CURS, label: "Curs" },
  { value: ActivityType.SEMINAR, label: "Seminar" },
  { value: ActivityType.LABORATOR, label: "Laborator" },
  { value: ActivityType.PROIECT, label: "Proiect" },
];

export const HOUR_TYPES: { value: HourType; label: string; color: string }[] = [
  { value: HourType.NORMA, label: "În normă", color: "#4CAF50" },
  { value: HourType.PLATA_ORA, label: "Plată cu ora", color: "#2196F3" },
];

export const TIME_SLOTS: string[] = [
  "08:00-09:00",
  "09:00-10:00",
  "10:00-11:00",
  "11:00-12:00",
  "12:00-13:00",
  "13:00-14:00",
  "14:00-15:00",
  "15:00-16:00",
  "16:00-17:00",
  "17:00-18:00",
  "18:00-19:00",
  "19:00-20:00",
  "20:00-21:00",
  "21:00-22:00",
];

export const MONTHS: string[] = [
  "Ianuarie",
  "Februarie",
  "Martie",
  "Aprilie",
  "Mai",
  "Iunie",
  "Iulie",
  "August",
  "Septembrie",
  "Octombrie",
  "Noiembrie",
  "Decembrie",
];
