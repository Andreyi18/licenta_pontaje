import React, { useState, useEffect, useCallback } from "react";
import {
  Box,
  Typography,
  Paper,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Divider,
  Card,
  CardContent,
  Grid,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from "@mui/material";
import {
  PictureAsPdf as PdfIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { timesheetsApi, documentsApi } from "../api/api";
import {
  AnnexType,
  TimesheetStatus,
  MONTHS,
  type Timesheet,
  type Document as Doc,
} from "../types";

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}

const statusLabel: Record<TimesheetStatus, string> = {
  [TimesheetStatus.DRAFT]: "Ciornă",
  [TimesheetStatus.SUBMITTED]: "Trimis",
  [TimesheetStatus.APPROVED]: "Aprobat",
};

const statusColor: Record<
  TimesheetStatus,
  "default" | "warning" | "primary" | "success"
> = {
  [TimesheetStatus.DRAFT]: "warning",
  [TimesheetStatus.SUBMITTED]: "primary",
  [TimesheetStatus.APPROVED]: "success",
};

const DocumentsPage: React.FC = () => {
  const now = new Date();
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());

  const [timesheet, setTimesheet] = useState<Timesheet | null>(null);
  const [documents, setDocuments] = useState<Doc[]>([]);
  const [loading, setLoading] = useState(false);
  const [generatingAnexa1, setGeneratingAnexa1] = useState(false);
  const [generatingAnexa3, setGeneratingAnexa3] = useState(false);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const years = Array.from({ length: 5 }, (_, i) => now.getFullYear() - 2 + i);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [ts, docs] = await Promise.all([
        timesheetsApi
          .getByPeriod(selectedMonth, selectedYear)
          .catch(() => null),
        documentsApi.getMine().catch(() => []),
      ]);
      setTimesheet(ts);
      // Filtrăm documentele aferente perioadei selectate
      const filtered = ts
        ? docs.filter((d: Doc) => d.timesheetId === ts.id)
        : [];
      setDocuments(filtered);
    } catch {
      toast.error("Eroare la încărcarea datelor");
    } finally {
      setLoading(false);
    }
  }, [selectedMonth, selectedYear]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleGenerate = async (annexType: AnnexType) => {
    if (!timesheet) return;
    const isAnexa1 = annexType === AnnexType.ANEXA_1;
    if (isAnexa1) setGeneratingAnexa1(true);
    else setGeneratingAnexa3(true);
    try {
      await documentsApi.generate(timesheet.id, annexType);
      toast.success(`Anexa ${isAnexa1 ? "1" : "3"} generată cu succes`);
      await loadData();
    } catch (err: any) {
      toast.error(err.message || "Eroare la generare");
    } finally {
      if (isAnexa1) setGeneratingAnexa1(false);
      else setGeneratingAnexa3(false);
    }
  };

  const handleDownload = async (doc: Doc) => {
    setDownloadingId(doc.id);
    try {
      const blob = await documentsApi.download(doc.id);
      downloadBlob(blob, doc.fileName);
    } catch (err: any) {
      toast.error(err.message || "Eroare la descărcare");
    } finally {
      setDownloadingId(null);
    }
  };

  const canGenerate =
    timesheet?.status === TimesheetStatus.SUBMITTED ||
    timesheet?.status === TimesheetStatus.APPROVED;

  const hasAnexa1 = documents.some((d) => d.annexType === AnnexType.ANEXA_1);
  const hasAnexa3 = documents.some((d) => d.annexType === AnnexType.ANEXA_3);

  return (
    <Box>
      {/* header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Documente & Anexe
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Generează și descarcă anexele salariale pentru pontajele tale
        </Typography>
      </Box>

      {/* selectare perioadă */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, flexWrap: "wrap" }}>
          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel>Lună</InputLabel>
            <Select
              value={selectedMonth}
              label="Lună"
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
            >
              {MONTHS.map((m, i) => (
                <MenuItem key={i + 1} value={i + 1}>
                  {m}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 100 }}>
            <InputLabel>An</InputLabel>
            <Select
              value={selectedYear}
              label="An"
              onChange={(e) => setSelectedYear(Number(e.target.value))}
            >
              {years.map((y) => (
                <MenuItem key={y} value={y}>
                  {y}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <IconButton onClick={loadData} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Box>
      </Paper>

      {loading ? (
        <Box sx={{ textAlign: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      ) : !timesheet ? (
        <Alert severity="info">
          Nu există pontaj pentru {MONTHS[selectedMonth - 1]} {selectedYear}.
          Creează un pontaj din secțiunea „Pontaj".
        </Alert>
      ) : (
        <>
          {/* info pontaj curent */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  flexWrap: "wrap",
                  gap: 2,
                }}
              >
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    Pontaj {MONTHS[selectedMonth - 1]} {selectedYear}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {timesheet.totalNormaHours}h normă ·{" "}
                    {timesheet.totalPlataOraHours}h plată cu ora ·{" "}
                    {timesheet.totalHours}h total
                  </Typography>
                </Box>
                <Chip
                  label={statusLabel[timesheet.status]}
                  color={statusColor[timesheet.status]}
                />
              </Box>

              {!canGenerate && (
                <Alert severity="warning" sx={{ mt: 2 }}>
                  Documentele pot fi generate doar după ce pontajul a fost
                  trimis la secretariat. Status actual: <strong>{statusLabel[timesheet.status]}</strong>
                </Alert>
              )}
            </CardContent>
          </Card>

          {/* generare anexe */}
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Generare anexe
            </Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box
                  sx={{
                    p: 3,
                    border: "2px solid",
                    borderColor: hasAnexa1 ? "success.main" : "grey.200",
                    borderRadius: 2,
                    textAlign: "center",
                  }}
                >
                  <PdfIcon
                    sx={{
                      fontSize: 48,
                      color: hasAnexa1 ? "success.main" : "grey.400",
                      mb: 1,
                    }}
                  />
                  <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
                    Anexa 1
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Situația orelor prestate în normă
                  </Typography>
                  <Button
                    variant={hasAnexa1 ? "outlined" : "contained"}
                    color={hasAnexa1 ? "success" : "primary"}
                    onClick={() => handleGenerate(AnnexType.ANEXA_1)}
                    disabled={!canGenerate || generatingAnexa1}
                    startIcon={
                      generatingAnexa1 ? (
                        <CircularProgress size={16} />
                      ) : (
                        <PdfIcon />
                      )
                    }
                    fullWidth
                  >
                    {hasAnexa1 ? "Regenerează Anexa 1" : "Generează Anexa 1"}
                  </Button>
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box
                  sx={{
                    p: 3,
                    border: "2px solid",
                    borderColor: hasAnexa3 ? "success.main" : "grey.200",
                    borderRadius: 2,
                    textAlign: "center",
                  }}
                >
                  <PdfIcon
                    sx={{
                      fontSize: 48,
                      color: hasAnexa3 ? "success.main" : "grey.400",
                      mb: 1,
                    }}
                  />
                  <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
                    Anexa 3
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Situația orelor prestate cu plată cu ora
                  </Typography>
                  <Button
                    variant={hasAnexa3 ? "outlined" : "contained"}
                    color={hasAnexa3 ? "success" : "primary"}
                    onClick={() => handleGenerate(AnnexType.ANEXA_3)}
                    disabled={!canGenerate || generatingAnexa3}
                    startIcon={
                      generatingAnexa3 ? (
                        <CircularProgress size={16} />
                      ) : (
                        <PdfIcon />
                      )
                    }
                    fullWidth
                  >
                    {hasAnexa3 ? "Regenerează Anexa 3" : "Generează Anexa 3"}
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </Paper>

          {/* lista documente */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Documente generate
            </Typography>
            <Divider sx={{ mb: 2 }} />
            {documents.length === 0 ? (
              <Box sx={{ textAlign: "center", py: 4 }}>
                <PdfIcon sx={{ fontSize: 48, color: "grey.300", mb: 1 }} />
                <Typography color="text.secondary">
                  Nu există documente generate pentru această perioadă.
                </Typography>
                {!canGenerate && (
                  <Typography variant="caption" color="text.disabled">
                    Trimite pontajul mai întâi pentru a putea genera anexele.
                  </Typography>
                )}
              </Box>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600 }}>Document</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Tip</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Generat la</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>
                        Acțiuni
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {documents.map((doc) => (
                      <TableRow key={doc.id} hover>
                        <TableCell>{doc.fileName}</TableCell>
                        <TableCell>
                          <Chip
                            label={
                              doc.annexType === AnnexType.ANEXA_1
                                ? "Anexa 1"
                                : "Anexa 3"
                            }
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          {new Date(doc.generatedAt).toLocaleString("ro-RO")}
                        </TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={
                              downloadingId === doc.id ? (
                                <CircularProgress size={14} />
                              ) : (
                                <DownloadIcon />
                              )
                            }
                            onClick={() => handleDownload(doc)}
                            disabled={downloadingId === doc.id}
                          >
                            Descarcă
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </>
      )}
    </Box>
  );
};

export default DocumentsPage;
