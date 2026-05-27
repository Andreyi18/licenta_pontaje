import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Grid,
  Card,
  CardContent,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  Chip,
  CircularProgress,
  IconButton,
  Avatar,
  Tooltip
} from "@mui/material";
import { 
  CheckCircle as CheckCircleIcon, 
  AccessTime as ClockIcon, 
  FilePresent as FileTextIcon, 
  Warning as AlertCircleIcon, 
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  Groups as UsersIcon,
  Visibility as ViewIcon
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { secretariatApi } from "../api/api";
import type { Timesheet } from "../types";
import { TimesheetStatus, MONTHS } from "../types";

const SecretariatDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  
  const [timesheets, setTimesheets] = useState<Timesheet[]>([]);
  const [stats, setStats] = useState({ total: 0, draft: 0, submitted: 0, approved: 0, missing: 0 });
  const [isLoading, setIsLoading] = useState(false);
  const [isMerging, setIsMerging] = useState(false);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  // Load Data
  useEffect(() => {
    fetchData();
  }, [selectedMonth, selectedYear, statusFilter]);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      // 1. Fetch Stats
      const statusData = await secretariatApi.getTimesheetStatus(selectedMonth, selectedYear);
      if (statusData) {
        setStats(statusData);
      }

      // 2. Fetch Timesheets
      const timesheetsData = await secretariatApi.getTimesheets(
        selectedMonth, 
        selectedYear, 
        statusFilter !== "ALL" ? { status: statusFilter } : undefined
      );
      if (Array.isArray(timesheetsData)) {
        setTimesheets(timesheetsData);
      }
    } catch (error) {
      toast.error("Eroare la preluarea datelor. Vă rugăm să reîncercați.");
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleMergeDocuments = async () => {
    setIsMerging(true);
    try {
      const blob = await secretariatApi.mergeDocuments(selectedMonth, selectedYear);
      
      const url = window.URL.createObjectURL(blob as Blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `pontaj_centralizator_${selectedMonth}_${selectedYear}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success("Documentele au fost concatenate cu succes!");
    } catch (error) {
      toast.error("Nu există documente pentru concatenare sau a apărut o eroare.");
      console.error(error);
    } finally {
      setIsMerging(false);
    }
  };

  const getStatusChip = (status: TimesheetStatus) => {
    switch (status) {
      case TimesheetStatus.APPROVED:
        return <Chip icon={<CheckCircleIcon />} label="Aprobat" color="success" size="small" variant="outlined" />;
      case TimesheetStatus.SUBMITTED:
        return <Chip icon={<CheckCircleIcon />} label="Trimis" color="primary" size="small" variant="outlined" />;
      case TimesheetStatus.DRAFT:
        return <Chip icon={<FileTextIcon />} label="Ciornă" color="default" size="small" variant="outlined" />;
      default:
        return <Chip label={status} size="small" variant="outlined" />;
    }
  };

  return (
    <Box sx={{ p: 1 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 4, flexWrap: "wrap", gap: 2 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Centralizator Secretariat
        </Typography>
        
        <Box sx={{ display: "flex", gap: 1 }}>
          <Tooltip title="Reîmprospătează datele">
            <IconButton onClick={fetchData} sx={{ border: "1px solid #ddd" }}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          
          <Button 
            variant="contained" 
            color="primary" 
            startIcon={isMerging ? <CircularProgress size={20} color="inherit" /> : <DownloadIcon />}
            onClick={handleMergeDocuments}
            disabled={isMerging || (stats.approved === 0 && stats.submitted === 0)}
          >
            {isMerging ? "Se generează..." : "Descarcă Centralizator PDF"}
          </Button>
        </Box>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 4, boxShadow: "0 2px 4px rgba(0,0,0,0.05)" }}>
        <CardContent sx={{ p: 2 }}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Luna</InputLabel>
                <Select
                  value={selectedMonth}
                  label="Luna"
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                >
                  {MONTHS.map((month, index) => (
                    <MenuItem key={index + 1} value={index + 1}>{month}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Anul</InputLabel>
                <Select
                  value={selectedYear}
                  label="Anul"
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                >
                  {[currentDate.getFullYear() - 1, currentDate.getFullYear(), currentDate.getFullYear() + 1].map(year => (
                    <MenuItem key={year} value={year}>{year}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => setStatusFilter(e.target.value as string)}
                >
                  <MenuItem value="ALL">Toate</MenuItem>
                  <MenuItem value="SUBMITTED">Trimise spre validare</MenuItem>
                  <MenuItem value="APPROVED">Aprobate</MenuItem>
                  <MenuItem value="DRAFT">Ciorne</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {[
          { label: "Total Profesori", value: stats.total, icon: <UsersIcon />, color: "#555" },
          { label: "Pontaje Trimise", value: stats.submitted, icon: <CheckCircleIcon />, color: "#2196F3" },
          { label: "Pontaje Aprobate", value: stats.approved, icon: <CheckCircleIcon />, color: "#4CAF50" },
          { label: "Lipsă / Ciornă", value: stats.missing + stats.draft, icon: <AlertCircleIcon />, color: "#F44336", extra: `(${stats.missing} lipsă, ${stats.draft} draft)` }
        ].map((card, idx) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={idx}>
            <Card sx={{ borderLeft: `4px solid ${card.color}` }}>
              <CardContent sx={{ display: "flex", alignItems: "center", p: 2 }}>
                <Box sx={{ color: card.color, mr: 2 }}>{card.icon}</Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{card.label}</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 700 }}>{card.value}</Typography>
                  {card.extra && <Typography variant="caption" color="text.secondary">{card.extra}</Typography>}
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Table */}
      <TableContainer component={Paper} sx={{ boxShadow: "0 2px 10px rgba(0,0,0,0.05)" }}>
        {isLoading ? (
          <Box sx={{ p: 8, textAlign: "center" }}>
            <CircularProgress />
            <Typography sx={{ mt: 2 }}>Se încarcă datele...</Typography>
          </Box>
        ) : timesheets.length === 0 ? (
          <Box sx={{ p: 8, textAlign: "center" }}>
            <Typography variant="h6" color="text.secondary">Nu s-au găsit date.</Typography>
          </Box>
        ) : (
          <Table>
            <TableHead sx={{ bgcolor: "#f9f9f9" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 600 }}>Cadru Didactic</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Normă</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Plată Ora</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Total Ore</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>Acțiuni</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {timesheets.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((ts) => (
                <TableRow key={ts.id} hover>
                  <TableCell>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                      <Avatar sx={{ bgcolor: "primary.light", width: 32, height: 32, fontSize: "0.875rem" }}>
                        {ts.userName?.charAt(0) || "U"}
                      </Avatar>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>{ts.userName || "Utilizator"}</Typography>
                    </Box>
                  </TableCell>
                  <TableCell>{ts.totalNormaHours} h</TableCell>
                  <TableCell>{ts.totalPlataOraHours} h</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>{ts.totalHours} h</TableCell>
                  <TableCell>{getStatusChip(ts.status)}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      startIcon={<ViewIcon />}
                      onClick={() =>
                        navigate(
                          `/secretariat/timesheets/${ts.id ?? "details"}`,
                          { state: { timesheet: ts } },
                        )
                      }
                    >
                      Vezi
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {!isLoading && timesheets.length > 0 && (
          <TablePagination
            component="div"
            count={timesheets.length}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(parseInt(e.target.value, 10));
              setPage(0);
            }}
            rowsPerPageOptions={[5, 10, 25]}
            labelRowsPerPage="Rânduri pe pagină:"
            labelDisplayedRows={({ from, to, count }) => `${from}–${to} din ${count}`}
          />
        )}
      </TableContainer>
    </Box>
  );
};

export default SecretariatDashboardPage;
