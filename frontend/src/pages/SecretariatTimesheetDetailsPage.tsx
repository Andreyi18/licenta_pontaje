import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import {
  Box,
  Typography,
  Card,
  CardContent,
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Breadcrumbs,
  Link as MUILink,
  Button,
} from "@mui/material";
import {
  ArrowBack as ArrowBackIcon,
  CheckCircle as ApproveIcon,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { secretariatApi } from "../api/api";
import type { Timesheet, TimesheetEntry } from "../types";
import { TimesheetStatus } from "../types";

const SecretariatTimesheetDetailsPage: React.FC = () => {
  const { timesheetId } = useParams<{ timesheetId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const navigationState = location.state as { timesheet?: Timesheet } | null;

  const [timesheet, setTimesheet] = useState<Timesheet | null>(
    navigationState?.timesheet ?? null,
  );
  const [isLoading, setIsLoading] = useState(false);
  const [approving, setApproving] = useState(false);

  useEffect(() => {
    const loadDetails = async () => {
      // dacă avem deja timesheet din state (din navigare), nu mai facem request
      if (timesheet || !timesheetId) {
        return;
      }
      setIsLoading(true);
      try {
        const data = await secretariatApi.getTimesheetDetails(timesheetId);
        setTimesheet(data);
      } catch (error) {
        console.error(error);
        toast.error("Eroare la încărcarea detaliilor pontajului.");
      } finally {
        setIsLoading(false);
      }
    };

    loadDetails();
  }, [timesheetId]);

  const getStatusChip = (status: TimesheetStatus) => {
    switch (status) {
      case TimesheetStatus.APPROVED:
        return (
          <Chip
            label="Aprobat"
            color="success"
            size="small"
            variant="outlined"
          />
        );
      case TimesheetStatus.SUBMITTED:
        return (
          <Chip
            label="Trimis"
            color="primary"
            size="small"
            variant="outlined"
          />
        );
      case TimesheetStatus.DRAFT:
      default:
        return (
          <Chip
            label="Ciornă"
            color="default"
            size="small"
            variant="outlined"
          />
        );
    }
  };

  const handleApprove = async () => {
    if (!timesheet) return;
    setApproving(true);
    try {
      const updated = await secretariatApi.approveTimesheet(timesheet.id);
      setTimesheet(updated);
      toast.success("Pontaj aprobat cu succes!");
    } catch (err: any) {
      toast.error(err.message || "Eroare la aprobare");
    } finally {
      setApproving(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("ro-RO");
  };

  const entries: TimesheetEntry[] = timesheet?.entries ?? [];

  return (
    <Box sx={{ p: 1 }}>
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          mb: 2,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <IconButton onClick={() => navigate(-1)} sx={{ mr: 1 }}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h5" sx={{ fontWeight: 600 }}>
            Detalii pontaj
          </Typography>
        </Box>
        {timesheet?.status === TimesheetStatus.SUBMITTED && (
          <Button
            variant="contained"
            color="success"
            startIcon={
              approving ? (
                <CircularProgress size={16} color="inherit" />
              ) : (
                <ApproveIcon />
              )
            }
            onClick={handleApprove}
            disabled={approving}
          >
            Aprobă pontajul
          </Button>
        )}
      </Box>

      <Breadcrumbs sx={{ mb: 2 }}>
        <MUILink
          component="button"
          underline="hover"
          color="inherit"
          onClick={() => navigate("/secretariat")}
        >
          Secretariat
        </MUILink>
        <Typography color="text.primary">Detalii pontaj</Typography>
      </Breadcrumbs>

      {isLoading ? (
        <Box sx={{ textAlign: "center", mt: 6 }}>
          <CircularProgress />
          <Typography sx={{ mt: 2 }}>Se încarcă detaliile pontajului...</Typography>
        </Box>
      ) : !timesheet ? (
        <Card>
          <CardContent>
            <Typography>
              Nu s-au găsit detalii pentru acest pontaj sau a apărut o eroare.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 1, fontWeight: 600 }}>
                {timesheet.userName} ({timesheet.userEmail})
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Departament: {timesheet.departmentName || "Nespecificat"}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Perioada: {timesheet.periodDisplay}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Total ore normă: {timesheet.totalNormaHours} h
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Total ore plată cu ora: {timesheet.totalPlataOraHours} h
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Total: {timesheet.totalHours} h
              </Typography>
              <Box sx={{ mt: 1 }}>{getStatusChip(timesheet.status)}</Box>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                Intrări în pontaj
              </Typography>
              {entries.length === 0 ? (
                <Typography color="text.secondary">
                  Nu există intrări pentru acest pontaj.
                </Typography>
              ) : (
                <TableContainer component={Paper}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Data</TableCell>
                        <TableCell>Zi</TableCell>
                        <TableCell>Interval</TableCell>
                        <TableCell>Tip oră</TableCell>
                        <TableCell>Activitate</TableCell>
                        <TableCell>Durată (h)</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {entries.map((entry) => (
                        <TableRow key={entry.id}>
                          <TableCell>{formatDate(entry.entryDate)}</TableCell>
                          <TableCell>{entry.dayOfWeek}</TableCell>
                          <TableCell>{entry.timeSlot}</TableCell>
                          <TableCell>{entry.hourTypeDisplay}</TableCell>
                          <TableCell>{entry.activity || "-"}</TableCell>
                          <TableCell>{entry.durationHours}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </Box>
  );
};

export default SecretariatTimesheetDetailsPage;

