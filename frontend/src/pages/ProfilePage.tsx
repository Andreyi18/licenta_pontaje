import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Paper,
  Grid,
  Avatar,
  TextField,
  Button,
  Divider,
  IconButton,
  Alert,
  Snackbar,
  CircularProgress,
  Chip,
} from "@mui/material";
import {
  Email as EmailIcon,
  School as SchoolIcon,
  Lock as LockIcon,
  Save as SaveIcon,
  Edit as EditIcon,
  Visibility,
  VisibilityOff,
} from "@mui/icons-material";
import { useAuth } from "../context/AuthContext";
import { authApi } from "../api/api";
import type { ProfileUpdateRequest, User } from "../types";

const roleLabels: Record<string, string> = {
  CADRU_DIDACTIC: "Cadru didactic",
  SECRETARIAT: "Secretariat",
  ADMIN: "Administrator",
};

const ProfilePage: React.FC = () => {
  const { user, updateUser } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

  // initializam form user
  useEffect(() => {
    if (user) {
      setFormData((prev) => ({
        ...prev,
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
      }));
    }
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // validare
    if (
      formData.newPassword &&
      formData.newPassword !== formData.confirmPassword
    ) {
      setError("Parolele noi nu se potrivesc");
      return;
    }

    if (formData.newPassword && !formData.currentPassword) {
      setError("Parola curentă este obligatorie pentru a seta o parolă nouă");
      return;
    }

    setIsSaving(true);
    try {
      const updateData: ProfileUpdateRequest = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        currentPassword: formData.currentPassword || undefined,
        newPassword: formData.newPassword || undefined,
      };

      const response = await authApi.updateProfile(updateData);

      // updatam local storage/context
      updateUser(response as User);

      // stergem campurile de parola dupa succes
      setFormData((prev) => ({
        ...prev,
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      }));

      setIsEditing(false);
      setShowSuccess(true);

      // update local storage/context
    } catch (err: any) {
      setError(err.message || "Eroare la actualizarea profilului");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 4 }}>
        Profilul Meu
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={4}>
        {/* card info user */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper
            sx={{
              textAlign: "center",
              height: "100%",
              overflow: "hidden",
              border: "1px solid rgba(0, 51, 102, 0.08)",
              boxShadow: "0 8px 32px rgba(15, 35, 65, 0.06)",
            }}
          >
            {/* banner gradient */}
            <Box
              sx={{
                height: 96,
                background:
                  "linear-gradient(135deg, #003366 0%, #0066cc 100%)",
              }}
            />
            <Box sx={{ px: 4, pb: 4, mt: "-56px" }}>
              <Avatar
                sx={{
                  width: 112,
                  height: 112,
                  fontSize: "2.75rem",
                  bgcolor: "primary.main",
                  mx: "auto",
                  mb: 2,
                  border: "4px solid #fff",
                  boxShadow: "0 6px 16px rgba(0, 51, 102, 0.25)",
                }}
              >
                {user?.firstName?.charAt(0)}
                {user?.lastName?.charAt(0)}
              </Avatar>
              <Typography variant="h5" sx={{ fontWeight: 700 }}>
                {user?.fullName}
              </Typography>
              <Box sx={{ mt: 1, mb: 1 }}>
                <Chip
                  label={roleLabels[user?.role ?? ""] || user?.role}
                  color="primary"
                  size="small"
                  variant="outlined"
                />
              </Box>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ textAlign: "left", mt: 2 }}>
              <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                <EmailIcon fontSize="small" color="primary" sx={{ mr: 2 }} />
                <Typography variant="body2">{user?.email}</Typography>
              </Box>
              <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                <SchoolIcon fontSize="small" color="primary" sx={{ mr: 2 }} />
                <Typography variant="body2">
                  {user?.departmentName || "Departament Nesetat"}
                </Typography>
              </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* setari si actiuni */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Box
            component="form"
            onSubmit={handleSave}
            sx={{ display: "flex", flexDirection: "column", gap: 3 }}
          >
            {/* detalii cont */}
            <Paper sx={{ p: 3 }}>
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  mb: 3,
                }}
              >
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Detalii Cont
                </Typography>
                {!isEditing ? (
                  <Button
                    startIcon={<EditIcon />}
                    variant="outlined"
                    onClick={() => setIsEditing(true)}
                  >
                    Editează
                  </Button>
                ) : (
                  <Box sx={{ display: "flex", gap: 1 }}>
                    <Button
                      variant="text"
                      onClick={() => setIsEditing(false)}
                      disabled={isSaving}
                    >
                      Anulează
                    </Button>
                    <Button
                      type="submit"
                      startIcon={
                        isSaving ? (
                          <CircularProgress size={20} color="inherit" />
                        ) : (
                          <SaveIcon />
                        )
                      }
                      variant="contained"
                      disabled={isSaving}
                    >
                      Salvează
                    </Button>
                  </Box>
                )}
              </Box>

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Prenume"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    disabled={!isEditing || isSaving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Nume"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    disabled={!isEditing || isSaving}
                  />
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <TextField
                    fullWidth
                    label="Email"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleChange}
                    disabled={!isEditing || isSaving}
                  />
                </Grid>
              </Grid>
            </Paper>

            {/* schimbare parola doar cand editezi */}
            {isEditing && (
              <Paper sx={{ p: 3 }}>
                <Box sx={{ display: "flex", alignItems: "center", mb: 3 }}>
                  <LockIcon color="primary" sx={{ mr: 1 }} />
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    Schimbare Parolă
                  </Typography>
                </Box>
                <Grid container spacing={3}>
                  <Grid size={{ xs: 12 }}>
                    <TextField
                      fullWidth
                      label="Parola Curentă"
                      name="currentPassword"
                      type={showCurrentPassword ? "text" : "password"}
                      value={formData.currentPassword}
                      onChange={handleChange}
                      placeholder="Introduceți parola curentă pentru a face modificări de securitate"
                      InputProps={{
                        endAdornment: (
                          <IconButton
                            onClick={() =>
                              setShowCurrentPassword(!showCurrentPassword)
                            }
                          >
                            {showCurrentPassword ? (
                              <VisibilityOff />
                            ) : (
                              <Visibility />
                            )}
                          </IconButton>
                        ),
                      }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Noua Parolă"
                      name="newPassword"
                      type={showNewPassword ? "text" : "password"}
                      value={formData.newPassword}
                      onChange={handleChange}
                      InputProps={{
                        endAdornment: (
                          <IconButton
                            onClick={() => setShowNewPassword(!showNewPassword)}
                          >
                            {showNewPassword ? (
                              <VisibilityOff />
                            ) : (
                              <Visibility />
                            )}
                          </IconButton>
                        ),
                      }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Confirmă Noua Parolă"
                      name="confirmPassword"
                      type="password"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                    />
                  </Grid>
                </Grid>
              </Paper>
            )}
          </Box>
        </Grid>
      </Grid>

      <Snackbar
        open={showSuccess}
        autoHideDuration={6000}
        onClose={() => setShowSuccess(false)}
      >
        <Alert
          onClose={() => setShowSuccess(false)}
          severity="success"
          sx={{ width: "100%" }}
        >
          Profilul a fost actualizat cu succes în baza de date!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ProfilePage;
