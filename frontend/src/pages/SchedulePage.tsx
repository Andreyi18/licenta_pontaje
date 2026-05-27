import React, { useState, useEffect, useRef } from "react";
import {
  Box,
  Typography,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Tooltip,
} from "@mui/material";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Upload as UploadIcon,
  DeleteSweep as DeleteAllIcon,
  CalendarMonth,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { schedulesApi } from "../api/api";
import {
  DayOfWeek,
  ActivityType,
  DAYS_OF_WEEK,
  ACTIVITY_TYPES,
  TIME_SLOTS,
  type Schedule,
} from "../types";

const activityColors: Record<ActivityType, string> = {
  [ActivityType.CURS]: "#003366",
  [ActivityType.SEMINAR]: "#2196F3",
  [ActivityType.LABORATOR]: "#4CAF50",
  [ActivityType.PROIECT]: "#FF9800",
};

// ----- Dialog Adaugă / Editează -----
interface ScheduleDialogProps {
  open: boolean;
  initial: Partial<Schedule> | null;
  existingSchedules: Schedule[];
  onClose: () => void;
  onSave: (data: Partial<Schedule>) => Promise<void>;
}

const ScheduleDialog: React.FC<ScheduleDialogProps> = ({
  open,
  initial,
  existingSchedules,
  onClose,
  onSave,
}) => {
  const [dayOfWeek, setDayOfWeek] = useState<DayOfWeek>(DayOfWeek.LUNI);
  const [timeSlot, setTimeSlot] = useState(TIME_SLOTS[0]);
  const [discipline, setDiscipline] = useState("");
  const [room, setRoom] = useState("");
  const [activityType, setActivityType] = useState<ActivityType>(
    ActivityType.CURS,
  );
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (initial) {
      setDayOfWeek(initial.dayOfWeek ?? DayOfWeek.LUNI);
      setTimeSlot(initial.timeSlot ?? TIME_SLOTS[0]);
      setDiscipline(initial.discipline ?? "");
      setRoom(initial.room ?? "");
      setActivityType(initial.activityType ?? ActivityType.CURS);
    } else {
      setDayOfWeek(DayOfWeek.LUNI);
      setTimeSlot(TIME_SLOTS[0]);
      setDiscipline("");
      setRoom("");
      setActivityType(ActivityType.CURS);
    }
  }, [initial, open]);

  const handleSave = async () => {
    if (!discipline.trim()) {
      toast.error("Disciplina este obligatorie");
      return;
    }
    setSaving(true);
    try {
      await onSave({ dayOfWeek, timeSlot, discipline, room, activityType });
      onClose();
    } catch {
      // eroarea e deja afișată în handleSave din SchedulePage
    } finally {
      setSaving(false);
    }
  };

  // Conflict: există deja o activitate în aceeași zi+interval (alta decât cea editată)
  const hasConflict = existingSchedules.some(
    (s) => s.dayOfWeek === dayOfWeek && s.timeSlot === timeSlot && s.id !== initial?.id,
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initial?.id ? "Editează activitate" : "Adaugă activitate"}</DialogTitle>
      <DialogContent dividers>
        {hasConflict && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Există deja o activitate în <strong>{DAYS_OF_WEEK.find((d) => d.value === dayOfWeek)?.label}</strong> la intervalul <strong>{timeSlot}</strong>. Salvarea va fi blocată de server.
          </Alert>
        )}
        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Zi</InputLabel>
          <Select
            value={dayOfWeek}
            label="Zi"
            onChange={(e) => setDayOfWeek(e.target.value as DayOfWeek)}
          >
            {DAYS_OF_WEEK.map((d) => (
              <MenuItem key={d.value} value={d.value}>
                {d.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Interval orar</InputLabel>
          <Select
            value={timeSlot}
            label="Interval orar"
            onChange={(e) => setTimeSlot(e.target.value)}
          >
            {TIME_SLOTS.map((slot) => (
              <MenuItem key={slot} value={slot}>
                {slot}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          fullWidth
          label="Disciplină"
          value={discipline}
          onChange={(e) => setDiscipline(e.target.value)}
          sx={{ mb: 2 }}
          required
        />

        <TextField
          fullWidth
          label="Sală (opțional)"
          value={room}
          onChange={(e) => setRoom(e.target.value)}
          sx={{ mb: 2 }}
          placeholder="ex: A201, Lab 3..."
        />

        <FormControl fullWidth>
          <InputLabel>Tip activitate</InputLabel>
          <Select
            value={activityType}
            label="Tip activitate"
            onChange={(e) => setActivityType(e.target.value as ActivityType)}
          >
            {ACTIVITY_TYPES.map((a) => (
              <MenuItem key={a.value} value={a.value}>
                {a.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Anulează</Button>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={saving}
          startIcon={saving ? <CircularProgress size={16} /> : undefined}
        >
          Salvează
        </Button>
      </DialogActions>
    </Dialog>
  );
};

// ----- Pagina principală -----
const SchedulePage: React.FC = () => {
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<Schedule | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [confirmDeleteAll, setConfirmDeleteAll] = useState(false);
  const csvRef = useRef<HTMLInputElement>(null);
  const excelRef = useRef<HTMLInputElement>(null);

  const loadSchedules = async () => {
    setLoading(true);
    try {
      const data = await schedulesApi.getMine();
      setSchedules(data);
    } catch {
      toast.error("Eroare la încărcarea orarului");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSchedules();
  }, []);

  const handleSave = async (data: Partial<Schedule>) => {
    try {
      if (editingSchedule?.id) {
        const updated = await schedulesApi.update(editingSchedule.id, data);
        setSchedules((prev) =>
          prev.map((s) => (s.id === updated.id ? updated : s)),
        );
        toast.success("Activitate actualizată");
      } else {
        const created = await schedulesApi.create(data);
        setSchedules((prev) => [...prev, created]);
        toast.success("Activitate adăugată");
      }
    } catch (err: any) {
      toast.error(err.message || "Eroare la salvare. Verificați datele introduse.");
      throw err; // re-throw ca dialogul să rămână deschis
    }
  };

  const handleDelete = async (id: string) => {
    setDeletingId(id);
    try {
      await schedulesApi.delete(id);
      setSchedules((prev) => prev.filter((s) => s.id !== id));
      toast.success("Activitate ștearsă");
    } catch (err: any) {
      toast.error(err.message || "Eroare la ștergere");
    } finally {
      setDeletingId(null);
    }
  };

  const handleDeleteAll = async () => {
    try {
      await schedulesApi.deleteAll();
      setSchedules([]);
      setConfirmDeleteAll(false);
      toast.success("Orar șters complet");
    } catch (err: any) {
      toast.error(err.message || "Eroare la ștergere");
    }
  };

  const handleImportCsv = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const imported = await schedulesApi.importCsv(file);
      setSchedules(imported);
      toast.success(`${imported.length} activități importate din CSV`);
    } catch (err: any) {
      toast.error(err.message || "Eroare la importul CSV");
    } finally {
      e.target.value = "";
    }
  };

  const handleImportExcel = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const imported = await schedulesApi.importExcel(file);
      setSchedules(imported);
      toast.success(`${imported.length} activități importate din Excel`);
    } catch (err: any) {
      toast.error(err.message || "Eroare la importul Excel");
    } finally {
      e.target.value = "";
    }
  };

  // Grupăm orarul pe zile
  const grouped = DAYS_OF_WEEK.reduce(
    (acc, day) => {
      acc[day.value] = schedules
        .filter((s) => s.dayOfWeek === day.value)
        .sort((a, b) => a.timeSlot.localeCompare(b.timeSlot));
      return acc;
    },
    {} as Record<DayOfWeek, Schedule[]>,
  );

  return (
    <Box>
      {/* header */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          mb: 3,
          flexWrap: "wrap",
          gap: 2,
        }}
      >
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Orar săptămânal
        </Typography>
        <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
          <input
            ref={csvRef}
            type="file"
            accept=".csv"
            style={{ display: "none" }}
            onChange={handleImportCsv}
          />
          <input
            ref={excelRef}
            type="file"
            accept=".xlsx,.xls"
            style={{ display: "none" }}
            onChange={handleImportExcel}
          />
          <Tooltip title="Importă orar din CSV">
            <Button
              variant="outlined"
              startIcon={<UploadIcon />}
              onClick={() => csvRef.current?.click()}
              size="small"
            >
              Import CSV
            </Button>
          </Tooltip>
          <Tooltip title="Importă orar din Excel">
            <Button
              variant="outlined"
              startIcon={<UploadIcon />}
              onClick={() => excelRef.current?.click()}
              size="small"
            >
              Import Excel
            </Button>
          </Tooltip>
          {schedules.length > 0 && (
            <Tooltip title="Șterge tot orarul">
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteAllIcon />}
                onClick={() => setConfirmDeleteAll(true)}
                size="small"
              >
                Șterge tot
              </Button>
            </Tooltip>
          )}
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setEditingSchedule(null);
              setDialogOpen(true);
            }}
          >
            Adaugă activitate
          </Button>
        </Box>
      </Box>

      {loading ? (
        <Box sx={{ textAlign: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      ) : schedules.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: "center" }}>
          <CalendarMonth sx={{ fontSize: 64, color: "grey.300", mb: 2 }} />
          <Typography variant="h6" color="text.secondary" sx={{ mb: 1 }}>
            Orarul este gol
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mb: 3 }}>
            Adaugă manual activitățile sau importă orarul dintr-un fișier CSV / Excel.
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setEditingSchedule(null); setDialogOpen(true); }}>
            Adaugă prima activitate
          </Button>
        </Paper>
      ) : (
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
          {DAYS_OF_WEEK.map((day) => {
            const daySchedules = grouped[day.value];
            if (daySchedules.length === 0) return null;
            return (
              <Paper key={day.value} sx={{ overflow: "hidden" }}>
                <Box
                  sx={{
                    px: 2,
                    py: 1.5,
                    backgroundColor: "primary.main",
                    color: "white",
                  }}
                >
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {day.label}
                  </Typography>
                </Box>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 600 }}>Interval</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Disciplină</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Sală</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Tip</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Ore</TableCell>
                        <TableCell align="right" />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {daySchedules.map((schedule) => (
                        <TableRow key={schedule.id} hover>
                          <TableCell>{schedule.timeSlot}</TableCell>
                          <TableCell sx={{ fontWeight: 500 }}>
                            {schedule.discipline}
                          </TableCell>
                          <TableCell>{schedule.room || "-"}</TableCell>
                          <TableCell>
                            <Chip
                              label={schedule.activityTypeDisplay}
                              size="small"
                              sx={{
                                backgroundColor:
                                  activityColors[schedule.activityType],
                                color: "white",
                                fontWeight: 600,
                              }}
                            />
                          </TableCell>
                          <TableCell>{schedule.durationHours}h</TableCell>
                          <TableCell align="right">
                            <IconButton
                              size="small"
                              onClick={() => {
                                setEditingSchedule(schedule);
                                setDialogOpen(true);
                              }}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDelete(schedule.id)}
                              disabled={deletingId === schedule.id}
                            >
                              {deletingId === schedule.id ? (
                                <CircularProgress size={16} />
                              ) : (
                                <DeleteIcon fontSize="small" />
                              )}
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            );
          })}

          {/* zile fara activitati */}
          {DAYS_OF_WEEK.filter((d) => grouped[d.value].length === 0).length > 0 && (
            <Typography variant="caption" color="text.secondary">
              Zile fără activități:{" "}
              {DAYS_OF_WEEK.filter((d) => grouped[d.value].length === 0)
                .map((d) => d.label)
                .join(", ")}
            </Typography>
          )}
        </Box>
      )}

      {/* dialog adaugare/editare */}
      <ScheduleDialog
        open={dialogOpen}
        initial={editingSchedule}
        existingSchedules={schedules}
        onClose={() => setDialogOpen(false)}
        onSave={handleSave}
      />

      {/* confirmare stergere totala */}
      <Dialog
        open={confirmDeleteAll}
        onClose={() => setConfirmDeleteAll(false)}
        maxWidth="xs"
      >
        <DialogTitle>Șterge tot orarul?</DialogTitle>
        <DialogContent>
          <Typography>
            Această acțiune va șterge toate cele {schedules.length} activități
            din orar și nu poate fi anulată.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDeleteAll(false)}>Anulează</Button>
          <Button variant="contained" color="error" onClick={handleDeleteAll}>
            Șterge tot
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SchedulePage;
