import React, { useState, useEffect, useCallback } from "react";
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  RadioGroup,
  FormControlLabel,
  Radio,
  FormLabel,
  CircularProgress,
  IconButton,
  Tooltip,
  Alert,
} from "@mui/material";
import {
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Send as SendIcon,
  CheckCircle as CheckIcon,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { timesheetsApi } from "../api/api";
import {
  HourType,
  TimesheetStatus,
  MONTHS,
  TIME_SLOTS,
  HOUR_TYPES,
  type Timesheet,
  type TimesheetEntry,
} from "../types";

// ----- helper: zilele unei luni -----
function getDaysInMonth(month: number, year: number): Date[] {
  const days: Date[] = [];
  const count = new Date(year, month, 0).getDate();
  for (let d = 1; d <= count; d++) {
    days.push(new Date(year, month - 1, d));
  }
  return days;
}

function dayName(date: Date): string {
  return date.toLocaleDateString("ro-RO", { weekday: "short" });
}

function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

const statusColor: Record<TimesheetStatus, "default" | "warning" | "primary" | "success"> = {
  [TimesheetStatus.DRAFT]: "warning",
  [TimesheetStatus.SUBMITTED]: "primary",
  [TimesheetStatus.APPROVED]: "success",
};

const statusLabel: Record<TimesheetStatus, string> = {
  [TimesheetStatus.DRAFT]: "Ciornă",
  [TimesheetStatus.SUBMITTED]: "Trimis",
  [TimesheetStatus.APPROVED]: "Aprobat",
};

// ----- Dialog adăugare oră -----
interface EntryDialogProps {
  open: boolean;
  date: Date | null;
  existingEntries: TimesheetEntry[];
  onClose: () => void;
  onAdd: (timeSlot: string, hourType: HourType, activity: string) => Promise<void>;
  onDelete: (entryId: string) => Promise<void>;
  readonly: boolean;
}

const EntryDialog: React.FC<EntryDialogProps> = ({
  open,
  date,
  existingEntries,
  onClose,
  onAdd,
  onDelete,
  readonly,
}) => {
  const [timeSlot, setTimeSlot] = useState(TIME_SLOTS[0]);
  const [hourType, setHourType] = useState<HourType>(HourType.NORMA);
  const [activity, setActivity] = useState("");
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleAdd = async () => {
    setSaving(true);
    try {
      await onAdd(timeSlot, hourType, activity);
      setActivity("");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (entryId: string) => {
    setDeletingId(entryId);
    try {
      await onDelete(entryId);
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {date
          ? date.toLocaleDateString("ro-RO", {
              weekday: "long",
              day: "numeric",
              month: "long",
              year: "numeric",
            })
          : ""}
      </DialogTitle>
      <DialogContent dividers>
        {/* ore existente */}
        {existingEntries.length > 0 && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
              Ore înregistrate
            </Typography>
            {existingEntries.map((entry) => (
              <Box
                key={entry.id}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  mb: 1,
                  p: 1.5,
                  borderRadius: 1,
                  backgroundColor: "grey.100",
                }}
              >
                <Box>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {entry.timeSlot}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 1, mt: 0.5 }}>
                    <Chip
                      label={entry.hourTypeDisplay}
                      size="small"
                      sx={{ backgroundColor: entry.hourTypeColor, color: "white" }}
                    />
                    {entry.activity && (
                      <Typography variant="caption" color="text.secondary">
                        {entry.activity}
                      </Typography>
                    )}
                  </Box>
                </Box>
                {!readonly && (
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleDelete(entry.id)}
                    disabled={deletingId === entry.id}
                  >
                    {deletingId === entry.id ? (
                      <CircularProgress size={16} />
                    ) : (
                      <DeleteIcon fontSize="small" />
                    )}
                  </IconButton>
                )}
              </Box>
            ))}
          </Box>
        )}

        {/* formular adăugare */}
        {!readonly && (
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 600 }}>
              Adaugă oră
            </Typography>
            {existingEntries.some((e) => e.timeSlot === timeSlot) && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                Intervalul <strong>{timeSlot}</strong> este deja înregistrat — salvarea va înlocui intrarea existentă.
              </Alert>
            )}
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

            <FormControl component="fieldset" sx={{ mb: 2 }}>
              <FormLabel component="legend">Tip oră</FormLabel>
              <RadioGroup
                row
                value={hourType}
                onChange={(e) => setHourType(e.target.value as HourType)}
              >
                {HOUR_TYPES.map((ht) => (
                  <FormControlLabel
                    key={ht.value}
                    value={ht.value}
                    control={<Radio />}
                    label={ht.label}
                  />
                ))}
              </RadioGroup>
            </FormControl>

            <TextField
              fullWidth
              label="Activitate (opțional)"
              value={activity}
              onChange={(e) => setActivity(e.target.value)}
              placeholder="ex: Curs Programare, Laborator SO..."
              size="small"
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>
          {readonly ? "Închide" : "Anulează"}
        </Button>
        {!readonly && (
          <Button
            variant="contained"
            onClick={handleAdd}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : <AddIcon />}
          >
            Adaugă
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

// ----- Pagina principală -----
const TimesheetPage: React.FC = () => {
  const now = new Date();
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());
  const [timesheet, setTimesheet] = useState<Timesheet | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // dialog
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedEntries, setSelectedEntries] = useState<TimesheetEntry[]>([]);

  const loadTimesheet = useCallback(async () => {
    setLoading(true);
    try {
      const ts = await timesheetsApi.getOrCreate(month, year);
      setTimesheet(ts);
    } catch (err) {
      toast.error("Eroare la încărcarea pontajului");
    } finally {
      setLoading(false);
    }
  }, [month, year]);

  useEffect(() => {
    loadTimesheet();
  }, [loadTimesheet]);

  const prevMonth = () => {
    if (month === 1) {
      setMonth(12);
      setYear((y) => y - 1);
    } else {
      setMonth((m) => m - 1);
    }
  };

  const nextMonth = () => {
    if (month === 12) {
      setMonth(1);
      setYear((y) => y + 1);
    } else {
      setMonth((m) => m + 1);
    }
  };

  const openDay = (date: Date) => {
    const iso = formatDateISO(date);
    const entries = timesheet?.entries?.filter((e) => e.entryDate === iso) ?? [];
    setSelectedDate(date);
    setSelectedEntries(entries);
    setDialogOpen(true);
  };

  const handleAdd = async (
    timeSlot: string,
    hourType: HourType,
    activity: string,
  ) => {
    if (!timesheet || !selectedDate) return;
    const iso = formatDateISO(selectedDate);
    // Detectăm dacă există deja o intrare pentru același interval orar
    const isUpdate = selectedEntries.some((e) => e.timeSlot === timeSlot);
    try {
      const updated = await timesheetsApi.addEntry(timesheet.id, {
        entryDate: iso,
        timeSlot,
        hourType,
        activity,
      });
      setTimesheet(updated);
      const newEntries = updated.entries?.filter((e) => e.entryDate === iso) ?? [];
      setSelectedEntries(newEntries);
      if (isUpdate) {
        toast("Intervalul era deja ocupat — intrarea a fost actualizată", {
          icon: "⚠️",
          style: { background: "#FF9800", color: "#fff" },
        });
      } else {
        toast.success("Oră adăugată cu succes");
      }
    } catch (err: any) {
      toast.error(err.message || "Eroare la adăugare");
    }
  };

  const handleDelete = async (entryId: string) => {
    if (!timesheet || !selectedDate) return;
    const iso = formatDateISO(selectedDate);
    try {
      const updated = await timesheetsApi.deleteEntry(timesheet.id, entryId);
      setTimesheet(updated);
      const newEntries = updated.entries?.filter((e) => e.entryDate === iso) ?? [];
      setSelectedEntries(newEntries);
      toast.success("Oră ștearsă");
    } catch (err: any) {
      toast.error(err.message || "Eroare la ștergere");
    }
  };

  const handleSubmit = async () => {
    if (!timesheet) return;
    setSubmitting(true);
    try {
      const updated = await timesheetsApi.submit(timesheet.id);
      setTimesheet(updated);
      toast.success("Pontaj trimis cu succes!");
    } catch (err: any) {
      toast.error(err.message || "Eroare la trimitere");
    } finally {
      setSubmitting(false);
    }
  };

  const days = getDaysInMonth(month, year);
  // padding la start (Luni = 1 în JS, duminica = 0)
  const firstDayJS = days[0].getDay(); // 0=Sun..6=Sat
  const paddingDays = firstDayJS === 0 ? 6 : firstDayJS - 1; // offset pentru Luni ca primul

  const isReadonly =
    timesheet?.status === TimesheetStatus.SUBMITTED ||
    timesheet?.status === TimesheetStatus.APPROVED;

  const canSubmit =
    timesheet?.status === TimesheetStatus.DRAFT &&
    (timesheet?.entries?.length ?? 0) > 0;

  const dayHeaders = ["Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum"];

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
          Pontaj lunar
        </Typography>
        {timesheet && (
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Chip
              label={statusLabel[timesheet.status]}
              color={statusColor[timesheet.status]}
              icon={
                timesheet.status === TimesheetStatus.APPROVED ? (
                  <CheckIcon />
                ) : undefined
              }
            />
            {canSubmit && (
              <Tooltip title="Trimite pontajul la secretariat">
                <Button
                  variant="contained"
                  color="success"
                  startIcon={
                    submitting ? <CircularProgress size={16} color="inherit" /> : <SendIcon />
                  }
                  onClick={handleSubmit}
                  disabled={submitting}
                >
                  Trimite pontajul
                </Button>
              </Tooltip>
            )}
          </Box>
        )}
      </Box>

      {/* selectare luna */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, flexWrap: "wrap" }}>
          <IconButton onClick={prevMonth}>
            <ChevronLeftIcon />
          </IconButton>
          <Typography variant="h6" sx={{ fontWeight: 600, minWidth: 180, textAlign: "center" }}>
            {MONTHS[month - 1]} {year}
          </Typography>
          <IconButton onClick={nextMonth}>
            <ChevronRightIcon />
          </IconButton>

          {timesheet && (
            <Box sx={{ ml: "auto", display: "flex", gap: 3 }}>
              <Box sx={{ textAlign: "center" }}>
                <Typography variant="h5" color="success.main" sx={{ fontWeight: 700 }}>
                  {timesheet.totalNormaHours}h
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Ore normă
                </Typography>
              </Box>
              <Box sx={{ textAlign: "center" }}>
                <Typography variant="h5" color="info.main" sx={{ fontWeight: 700 }}>
                  {timesheet.totalPlataOraHours}h
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Plată cu ora
                </Typography>
              </Box>
              <Box sx={{ textAlign: "center" }}>
                <Typography variant="h5" color="primary.main" sx={{ fontWeight: 700 }}>
                  {timesheet.totalHours}h
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Total
                </Typography>
              </Box>
            </Box>
          )}
        </Box>
      </Paper>

      {isReadonly && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {timesheet?.status === TimesheetStatus.SUBMITTED
            ? "Pontajul a fost trimis și nu mai poate fi modificat."
            : "Pontajul a fost aprobat."}
        </Alert>
      )}

      {/* calendar */}
      {loading ? (
        <Box sx={{ textAlign: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper sx={{ p: 2 }}>
          {/* header zile */}
          <Grid container>
            {dayHeaders.map((d) => (
              <Grid size={{ xs: 12 / 7 }} key={d}>
                <Box
                  sx={{
                    textAlign: "center",
                    py: 1,
                    fontWeight: 600,
                    fontSize: "0.8rem",
                    color: "text.secondary",
                  }}
                >
                  {d}
                </Box>
              </Grid>
            ))}
          </Grid>

          {/* celulele calendarului */}
          <Grid container>
            {/* padding pentru prima zi */}
            {Array.from({ length: paddingDays }).map((_, i) => (
              <Grid size={{ xs: 12 / 7 }} key={`pad-${i}`}>
                <Box sx={{ minHeight: 80 }} />
              </Grid>
            ))}

            {days.map((date) => {
              const iso = formatDateISO(date);
              const dayEntries =
                timesheet?.entries?.filter((e) => e.entryDate === iso) ?? [];
              const isWeekend = date.getDay() === 0 || date.getDay() === 6;
              const isToday = iso === formatDateISO(new Date());

              return (
                <Grid size={{ xs: 12 / 7 }} key={iso}>
                  <Box
                    onClick={() => openDay(date)}
                    sx={{
                      minHeight: 80,
                      border: "1px solid",
                      borderColor: isToday ? "primary.main" : "grey.200",
                      borderRadius: 1,
                      m: 0.3,
                      p: 0.5,
                      cursor: "pointer",
                      backgroundColor: isWeekend
                        ? "grey.50"
                        : isToday
                          ? "primary.50"
                          : "white",
                      "&:hover": {
                        backgroundColor: "grey.100",
                        borderColor: "primary.light",
                      },
                      transition: "all 0.15s",
                    }}
                  >
                    <Typography
                      variant="caption"
                      sx={{
                        fontWeight: isToday ? 700 : 400,
                        color: isToday ? "primary.main" : "text.primary",
                      }}
                    >
                      {date.getDate()}
                    </Typography>
                    <Box sx={{ mt: 0.5 }}>
                      {dayEntries.slice(0, 3).map((entry) => (
                        <Box
                          key={entry.id}
                          sx={{
                            fontSize: "0.6rem",
                            backgroundColor: entry.hourTypeColor,
                            color: "white",
                            borderRadius: 0.5,
                            px: 0.5,
                            mb: 0.3,
                            overflow: "hidden",
                            whiteSpace: "nowrap",
                            textOverflow: "ellipsis",
                          }}
                        >
                          {entry.timeSlot}
                        </Box>
                      ))}
                      {dayEntries.length > 3 && (
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ fontSize: "0.6rem" }}
                        >
                          +{dayEntries.length - 3} mai multe
                        </Typography>
                      )}
                    </Box>
                  </Box>
                </Grid>
              );
            })}
          </Grid>

          {!isReadonly && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mt: 2, textAlign: "center" }}
            >
              Click pe o zi pentru a adăuga sau vizualiza ore
            </Typography>
          )}
        </Paper>
      )}

      {/* dialog zi */}
      <EntryDialog
        open={dialogOpen}
        date={selectedDate}
        existingEntries={selectedEntries}
        onClose={() => setDialogOpen(false)}
        onAdd={handleAdd}
        onDelete={handleDelete}
        readonly={isReadonly}
      />
    </Box>
  );
};

export default TimesheetPage;
