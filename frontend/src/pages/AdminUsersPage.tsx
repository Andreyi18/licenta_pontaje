import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Paper,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  IconButton,
  Avatar,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Alert,
  InputAdornment,
  Tooltip,
} from "@mui/material";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  PersonOff as DeactivateIcon,
  PersonAdd as ActivateIcon,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { usersApi } from "../api/api";
import {
  UserRole,
  UserStatus,
  type User,
} from "../types";

const roleLabels: Record<UserRole, string> = {
  [UserRole.CADRU_DIDACTIC]: "Cadru Didactic",
  [UserRole.SECRETARIAT]: "Secretariat",
  [UserRole.ADMIN]: "Administrator",
};

const roleColors: Record<
  UserRole,
  "default" | "primary" | "secondary" | "error"
> = {
  [UserRole.CADRU_DIDACTIC]: "primary",
  [UserRole.SECRETARIAT]: "secondary",
  [UserRole.ADMIN]: "error",
};

// ----- Dialog creare/editare utilizator -----
interface UserDialogProps {
  open: boolean;
  user: User | null;
  onClose: () => void;
  onSave: (data: Partial<User> & { password?: string }) => Promise<void>;
}

const UserDialog: React.FC<UserDialogProps> = ({
  open,
  user,
  onClose,
  onSave,
}) => {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<UserRole>(UserRole.CADRU_DIDACTIC);
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setFirstName(user.firstName);
      setLastName(user.lastName);
      setEmail(user.email);
      setRole(user.role);
      setPassword("");
    } else {
      setFirstName("");
      setLastName("");
      setEmail("");
      setRole(UserRole.CADRU_DIDACTIC);
      setPassword("");
    }
  }, [user, open]);

  const handleSave = async () => {
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      toast.error("Completați toate câmpurile obligatorii");
      return;
    }
    if (!user && !password.trim()) {
      toast.error("Parola este obligatorie pentru utilizatori noi");
      return;
    }
    setSaving(true);
    try {
      const data: Partial<User> & { password?: string } = {
        firstName,
        lastName,
        email,
        role,
      };
      if (password) data.password = password;
      await onSave(data);
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {user ? "Editează utilizator" : "Adaugă utilizator nou"}
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
          <TextField
            label="Prenume"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            fullWidth
            required
          />
          <TextField
            label="Nume"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            fullWidth
            required
          />
        </Box>

        <TextField
          label="Email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          fullWidth
          required
          sx={{ mb: 2 }}
        />

        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Rol</InputLabel>
          <Select
            value={role}
            label="Rol"
            onChange={(e) => setRole(e.target.value as UserRole)}
          >
            {Object.entries(roleLabels).map(([val, label]) => (
              <MenuItem key={val} value={val}>
                {label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          label={user ? "Parolă nouă (lăsați gol pentru a nu schimba)" : "Parolă"}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          fullWidth
          required={!user}
        />
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
const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterRole, setFilterRole] = useState<string>("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<User | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await usersApi.getAll({
        role: filterRole || undefined,
        search: search || undefined,
      });
      setUsers(data);
    } catch {
      toast.error("Eroare la încărcarea utilizatorilor");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterRole]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadUsers();
  };

  const handleSave = async (data: Partial<User> & { password?: string }) => {
    if (editingUser) {
      const updated = await usersApi.update(editingUser.id, data);
      setUsers((prev) =>
        prev.map((u) => (u.id === updated.id ? updated : u)),
      );
      toast.success("Utilizator actualizat");
    } else {
      const created = await usersApi.create(data);
      setUsers((prev) => [...prev, created]);
      toast.success("Utilizator creat");
    }
  };

  const handleToggleStatus = async (user: User) => {
    setTogglingId(user.id);
    try {
      const newStatus =
        user.status === UserStatus.ACTIVE
          ? UserStatus.INACTIVE
          : UserStatus.ACTIVE;
      const updated = await usersApi.updateStatus(user.id, newStatus);
      setUsers((prev) =>
        prev.map((u) => (u.id === updated.id ? updated : u)),
      );
      toast.success(
        newStatus === UserStatus.ACTIVE
          ? "Utilizator activat"
          : "Utilizator dezactivat",
      );
    } catch (err: any) {
      toast.error(err.message || "Eroare");
    } finally {
      setTogglingId(null);
    }
  };

  const handleDelete = async (user: User) => {
    setDeletingId(user.id);
    try {
      await usersApi.delete(user.id);
      setUsers((prev) => prev.filter((u) => u.id !== user.id));
      setConfirmDelete(null);
      toast.success("Utilizator șters");
    } catch (err: any) {
      toast.error(err.message || "Eroare la ștergere");
    } finally {
      setDeletingId(null);
    }
  };

  const filtered = users.filter((u) => {
    if (!search) return true;
    const s = search.toLowerCase();
    return (
      u.firstName.toLowerCase().includes(s) ||
      u.lastName.toLowerCase().includes(s) ||
      u.email.toLowerCase().includes(s)
    );
  });

  const paginated = filtered.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

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
          Gestionare utilizatori
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            setEditingUser(null);
            setDialogOpen(true);
          }}
        >
          Adaugă utilizator
        </Button>
      </Box>

      {/* filtre */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box
          component="form"
          onSubmit={handleSearch}
          sx={{ display: "flex", gap: 2, flexWrap: "wrap", alignItems: "center" }}
        >
          <TextField
            placeholder="Caută după nume sau email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            size="small"
            sx={{ flexGrow: 1, minWidth: 200 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>Filtrează după rol</InputLabel>
            <Select
              value={filterRole}
              label="Filtrează după rol"
              onChange={(e) => setFilterRole(e.target.value)}
            >
              <MenuItem value="">Toate rolurile</MenuItem>
              {Object.entries(roleLabels).map(([val, label]) => (
                <MenuItem key={val} value={val}>
                  {label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button type="submit" variant="outlined">
            Caută
          </Button>
        </Box>
      </Paper>

      {/* tabel */}
      {loading ? (
        <Box sx={{ textAlign: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      ) : filtered.length === 0 ? (
        <Alert severity="info">Nu s-au găsit utilizatori cu criteriile selectate.</Alert>
      ) : (
        <Paper>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Utilizator</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Email</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Rol</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Departament</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>
                    Acțiuni
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paginated.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                        <Avatar sx={{ bgcolor: "primary.main", width: 34, height: 34, fontSize: "0.85rem" }}>
                          {user.firstName.charAt(0)}
                          {user.lastName.charAt(0)}
                        </Avatar>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {user.firstName} {user.lastName}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Chip
                        label={roleLabels[user.role]}
                        color={roleColors[user.role]}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      {user.departmentName || "-"}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={
                          user.status === UserStatus.ACTIVE
                            ? "Activ"
                            : "Inactiv"
                        }
                        color={
                          user.status === UserStatus.ACTIVE
                            ? "success"
                            : "default"
                        }
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip
                        title={
                          user.status === UserStatus.ACTIVE
                            ? "Dezactivează"
                            : "Activează"
                        }
                      >
                        <IconButton
                          size="small"
                          color={
                            user.status === UserStatus.ACTIVE
                              ? "warning"
                              : "success"
                          }
                          onClick={() => handleToggleStatus(user)}
                          disabled={togglingId === user.id}
                        >
                          {togglingId === user.id ? (
                            <CircularProgress size={16} />
                          ) : user.status === UserStatus.ACTIVE ? (
                            <DeactivateIcon fontSize="small" />
                          ) : (
                            <ActivateIcon fontSize="small" />
                          )}
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Editează">
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditingUser(user);
                            setDialogOpen(true);
                          }}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Șterge">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => setConfirmDelete(user)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={filtered.length}
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
        </Paper>
      )}

      {/* dialog creare/editare */}
      <UserDialog
        open={dialogOpen}
        user={editingUser}
        onClose={() => setDialogOpen(false)}
        onSave={handleSave}
      />

      {/* confirmare stergere */}
      <Dialog
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        maxWidth="xs"
      >
        <DialogTitle>Șterge utilizatorul?</DialogTitle>
        <DialogContent>
          <Typography>
            Ești sigur că vrei să ștergi utilizatorul{" "}
            <strong>
              {confirmDelete?.firstName} {confirmDelete?.lastName}
            </strong>
            ? Această acțiune nu poate fi anulată.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Anulează</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => confirmDelete && handleDelete(confirmDelete)}
            disabled={!!deletingId}
            startIcon={deletingId ? <CircularProgress size={16} /> : undefined}
          >
            Șterge
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AdminUsersPage;
