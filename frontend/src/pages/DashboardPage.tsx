import React, { useState, useEffect, useMemo } from "react";
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  LinearProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Button,
  CircularProgress,
} from "@mui/material";
import {
  AccessTime as TimesheetIcon,
  CalendarMonth as CalendarIcon,
  Description as DocumentIcon,
  TrendingUp as TrendingIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  Schedule as ClockIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  authApi,
  timesheetsApi,
  schedulesApi,
  documentsApi,
  secretariatApi,
} from "../api/api";
import {
  UserRole,
  MONTHS,
  type Timesheet,
  type Schedule,
  type Document,
} from "../types";

const DashboardPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);

  // state date
  const [currentTimesheet, setCurrentTimesheet] = useState<Timesheet | null>(
    null,
  );
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [secretariatStats, setSecretariatStats] = useState<{
    total: number;
    draft: number;
    submitted: number;
    approved: number;
    missing: number;
  } | null>(null);

  const currentMonth = new Date().getMonth();
  const currentYear = new Date().getFullYear();

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // dam fetch la date in paralel
        const monthForApi = currentMonth + 1; // backend foloseste 1-12

        const promises: Promise<any>[] = [
          timesheetsApi.getOrCreate(monthForApi, currentYear).catch(() => null),
          schedulesApi.getMine().catch(() => []),
          documentsApi.getMine().catch(() => []),
        ];

        if (hasRole([UserRole.SECRETARIAT, UserRole.ADMIN])) {
          promises.push(
            secretariatApi
              .getTimesheetStatus(monthForApi, currentYear)
              .catch(() => null),
          );
        }

        const [ts, sch, docs, secStats] = await Promise.all(promises);

        setCurrentTimesheet(ts);
        setSchedules(sch);
        setDocuments(docs);
        if (secStats) setSecretariatStats(secStats);
      } catch (error) {
        console.error("Error fetching dashboard data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [currentMonth, currentYear, hasRole]);

  // statistici derivate
  const stats = useMemo(() => {
    return [
      {
        title: "Ore în normă",
        value: currentTimesheet?.totalNormaHours?.toString() || "0",
        subtitle: `${MONTHS[currentMonth]} ${currentYear}`,
        icon: <TimesheetIcon />,
        color: "#4CAF50",
      },
      {
        title: "Ore plată cu ora",
        value: currentTimesheet?.totalPlataOraHours?.toString() || "0",
        subtitle: `${MONTHS[currentMonth]} ${currentYear}`,
        icon: <ClockIcon />,
        color: "#2196F3",
      },
      {
        title: "Activități orar",
        value: schedules.length.toString(),
        subtitle: "Total activități",
        icon: <CalendarIcon />,
        color: "#FF9800",
      },
      {
        title: "Documente",
        value: documents.length.toString(),
        subtitle: "Total generate",
        icon: <DocumentIcon />,
        color: "#9C27B0",
      },
    ];
  }, [currentTimesheet, schedules, documents, currentMonth, currentYear]);

  // parser activitati
  const activities = useMemo(() => {
    const act: any[] = [];

    if (currentTimesheet?.status === "SUBMITTED") {
      act.push({
        text: `Pontaj trimis pentru ${MONTHS[currentMonth]} ${currentYear}`,
        status: "success",
        time: "Recent",
      });
    } else if (currentTimesheet) {
      act.push({
        text: `Pontaj în curs pentru ${MONTHS[currentMonth]} ${currentYear}`,
        status: "pending",
        time: "În lucru",
      });
    }

    documents.slice(0, 2).forEach((doc) => {
      act.push({
        text: `Document generat: ${doc.fileName}`,
        status: "success",
        time: new Date(doc.generatedAt).toLocaleDateString(),
      });
    });

    if (act.length === 0) {
      act.push({ text: "Nicio activitate recentă", status: "none", time: "-" });
    }

    return act;
  }, [currentTimesheet, documents, currentMonth, currentYear]);

  const quickActions = hasRole([UserRole.CADRU_DIDACTIC])
    ? [
        {
          title: "Completează pontajul",
          path: "/timesheet",
          icon: <TimesheetIcon />,
        },
        { title: "Vezi orarul", path: "/schedule", icon: <CalendarIcon /> },
        { title: "Generează Anexă", path: "/documents", icon: <DocumentIcon /> },
      ]
    : [
        {
          title: "Vezi centralizatorul",
          path: "/secretariat",
          icon: <TimesheetIcon />,
        },
      ];

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "80vh",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* header bun venit */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          Bine ai venit, {user?.firstName}! 👋
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {new Date().toLocaleDateString("ro-RO", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
          })}
        </Typography>
      </Box>

      {/* carduri statistici */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {stats.map((stat, index) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={index}>
            <Card
              sx={{
                height: "100%",
                background: `linear-gradient(135deg, ${stat.color}15 0%, ${stat.color}05 100%)`,
                border: `1px solid ${stat.color}20`,
              }}
            >
              <CardContent>
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    mb: 2,
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      backgroundColor: stat.color,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "white",
                    }}
                  >
                    {stat.icon}
                  </Box>
                  <TrendingIcon sx={{ color: stat.color }} />
                </Box>
                <Typography variant="h3" sx={{ fontWeight: 700, mb: 0.5 }}>
                  {stat.value}
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ fontWeight: 500 }}
                >
                  {stat.title}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {stat.subtitle}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        {/* progres pontaj */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 3 }}>
              Progres Pontaj - {MONTHS[currentMonth]} {currentYear}
            </Typography>

            <Box sx={{ mb: 3 }}>
              <Box
                sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}
              >
                <Typography variant="body2">Total ore înregistrate</Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {currentTimesheet?.totalHours || 0} ore
                </Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={Math.min(
                  ((currentTimesheet?.totalHours || 0) / 40) * 100,
                  100,
                )} // Folosim 40 ca target
                sx={{
                  height: 10,
                  borderRadius: 5,
                  backgroundColor: "grey.200",
                  "& .MuiLinearProgress-bar": {
                    borderRadius: 5,
                    background:
                      "linear-gradient(90deg, #4CAF50 0%, #8BC34A 100%)",
                  },
                }}
              />
            </Box>

            <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
              <Chip
                icon={<CheckIcon />}
                label={`${currentTimesheet?.totalNormaHours || 0} ore în normă`}
                color="success"
                variant="outlined"
              />
              <Chip
                icon={<ClockIcon />}
                label={`${currentTimesheet?.totalPlataOraHours || 0} ore plată cu ora`}
                color="info"
                variant="outlined"
              />
              <Chip
                icon={<WarningIcon />}
                label={`Status: ${currentTimesheet?.statusDisplay || "Nedeterminat"}`}
                color={
                  currentTimesheet?.status === "APPROVED"
                    ? "success"
                    : "warning"
                }
                variant="outlined"
              />
            </Box>

            {/* actiuni rapide */}
            <Box sx={{ mt: 4, display: "flex", gap: 2, flexWrap: "wrap" }}>
              {quickActions.map((action, index) => (
                <Button
                  key={index}
                  variant="outlined"
                  startIcon={action.icon}
                  sx={{ flexGrow: { xs: 1, sm: 0 } }}
                  onClick={() => navigate(action.path)}
                >
                  {action.title}
                </Button>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* activitati recente */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper sx={{ p: 3, height: "100%" }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Activitate Recentă
            </Typography>
            <List disablePadding>
              {activities.map((item, index) => (
                <ListItem key={index} disablePadding sx={{ mb: 2 }}>
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    {item.status === "success" ? (
                      <CheckIcon color="success" fontSize="small" />
                    ) : item.status === "pending" ? (
                      <ClockIcon color="warning" fontSize="small" />
                    ) : (
                      <Box sx={{ width: 20 }} />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={item.text}
                    secondary={item.time}
                    primaryTypographyProps={{ variant: "body2" }}
                    secondaryTypographyProps={{ variant: "caption" }}
                  />
                </ListItem>
              ))}
            </List>
          </Paper>
        </Grid>

        {/* dashboard sectiune secretariat */}
        {hasRole([UserRole.SECRETARIAT, UserRole.ADMIN]) &&
          secretariatStats && (
            <Grid size={{ xs: 12 }}>
              <Paper
                sx={{
                  p: 3,
                  background:
                    "linear-gradient(135deg, #003366 0%, #0066cc 100%)",
                  color: "white",
                }}
              >
                <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
                  Panou Secretariat - {MONTHS[currentMonth]} {currentYear}
                </Typography>
                <Grid container spacing={3}>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <Box sx={{ textAlign: "center", p: 2 }}>
                      <Typography variant="h2" sx={{ fontWeight: 700 }}>
                        {secretariatStats.submitted + secretariatStats.approved}
                      </Typography>
                      <Typography variant="body2">Pontaje primite</Typography>
                    </Box>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <Box sx={{ textAlign: "center", p: 2 }}>
                      <Typography variant="h2" sx={{ fontWeight: 700 }}>
                        {secretariatStats.submitted}
                      </Typography>
                      <Typography variant="body2">În așteptare</Typography>
                    </Box>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <Box sx={{ textAlign: "center", p: 2 }}>
                      <Typography variant="h2" sx={{ fontWeight: 700 }}>
                        {secretariatStats.draft}
                      </Typography>
                      <Typography variant="body2">În lucru (Draft)</Typography>
                    </Box>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <Box sx={{ textAlign: "center", p: 2 }}>
                      <Typography variant="h2" sx={{ fontWeight: 700 }}>
                        {secretariatStats.missing}
                      </Typography>
                      <Typography variant="body2">Lipsă record</Typography>
                    </Box>
                  </Grid>
                </Grid>
                <Box sx={{ mt: 2, textAlign: "center" }}>
                  <Button
                    variant="contained"
                    color="inherit"
                    sx={{ color: "#003366" }}
                    onClick={() => navigate("/secretariat")}
                  >
                    Vezi Centralizator
                  </Button>
                </Box>
              </Paper>
            </Grid>
          )}
      </Grid>
    </Box>
  );
};

export default DashboardPage;
