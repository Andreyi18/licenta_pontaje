import React, { useState, useEffect, useCallback } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
  useMediaQuery,
  Badge,
  Popover,
  Button,
  Chip,
} from "@mui/material";
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  CalendarMonth as CalendarIcon,
  AccessTime as TimesheetIcon,
  Description as DocumentIcon,
  People as PeopleIcon,
  Notifications as NotificationIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
  AccountCircle as AccountIcon,
  ChevronLeft as ChevronLeftIcon,
  DoneAll as DoneAllIcon,
} from "@mui/icons-material";
import { useAuth } from "../../context/AuthContext";
import { UserRole, type AppNotification } from "../../types";
import { notificationsApi } from "../../api/api";

const DRAWER_WIDTH = 260;

interface NavItem {
  title: string;
  path: string;
  icon: React.ReactNode;
  roles?: UserRole[];
}

const navItems: NavItem[] = [
  {
    title: "Dashboard",
    path: "/dashboard",
    icon: <DashboardIcon />,
  },
  {
    title: "Orar",
    path: "/schedule",
    icon: <CalendarIcon />,
    roles: [UserRole.CADRU_DIDACTIC],
  },
  {
    title: "Pontaj",
    path: "/timesheet",
    icon: <TimesheetIcon />,
    roles: [UserRole.CADRU_DIDACTIC],
  },
  {
    title: "Documente",
    path: "/documents",
    icon: <DocumentIcon />,
    roles: [UserRole.CADRU_DIDACTIC],
  },
  {
    title: "Centralizator",
    path: "/secretariat",
    icon: <PeopleIcon />,
    roles: [UserRole.SECRETARIAT, UserRole.ADMIN],
  },
  {
    title: "Utilizatori",
    path: "/admin/users",
    icon: <PeopleIcon />,
    roles: [UserRole.ADMIN],
  },
];

const MainLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, hasRole } = useAuth();

  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  // notificari
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [notifAnchor, setNotifAnchor] = useState<null | HTMLElement>(null);
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const loadNotifications = useCallback(async () => {
    try {
      const data = await notificationsApi.getAll();
      setNotifications(data);
    } catch {
      // silent
    }
  }, []);

  useEffect(() => {
    loadNotifications();
    // poll la fiecare 60 secunde
    const interval = setInterval(loadNotifications, 60000);
    return () => clearInterval(interval);
  }, [loadNotifications]);

  const handleNotifOpen = (e: React.MouseEvent<HTMLElement>) => {
    setNotifAnchor(e.currentTarget);
    loadNotifications();
  };

  const handleNotifClose = () => setNotifAnchor(null);

  const handleMarkAllRead = async () => {
    await notificationsApi.markAllAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  const handleMarkRead = async (id: string) => {
    await notificationsApi.markAsRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)),
    );
  };

  const handleDrawerToggle = () => setMobileOpen(!mobileOpen);
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) =>
    setAnchorEl(event.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleNavClick = (path: string) => {
    navigate(path);
    if (isMobile) setMobileOpen(false);
  };

  const handleLogout = () => {
    handleMenuClose();
    logout();
    navigate("/login");
  };

  // filtram bara de navigare in functie de rolurile utilizatorului
  const filteredNavItems = navItems.filter((item) => {
    if (!item.roles) return true;
    return hasRole(item.roles);
  });

  const drawer = (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      {/* logo si header */}
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          p: 2,
          minHeight: 100,
          background: "linear-gradient(135deg, #003366 0%, #004d99 100%)",
          color: "white",
          position: "relative",
        }}
      >
        <Box
          component="img"
          src="/logo-upt.jpg"
          alt="UPT Logo"
          sx={{
            width: "90%",
            maxWidth: 200,
            height: "auto",
            borderRadius: "8px",
            objectFit: "contain",
            mb: 1,
          }}
        />
        <Typography
          variant="h6"
          sx={{
            fontWeight: 700,
            letterSpacing: 2,
            textTransform: "uppercase",
            fontSize: "1rem",
          }}
        >
          Sistem Pontaje
        </Typography>
        {isMobile && (
          <IconButton
            onClick={handleDrawerToggle}
            sx={{ color: "white", position: "absolute", right: 8, top: 8 }}
          >
            <ChevronLeftIcon />
          </IconButton>
        )}
      </Box>

      <Divider />

      {/* navigarea */}
      <List sx={{ flexGrow: 1, px: 1, py: 2 }}>
        {filteredNavItems.map((item) => (
          <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => handleNavClick(item.path)}
              selected={location.pathname === item.path}
              sx={{
                borderRadius: 2,
                "&.Mui-selected": {
                  backgroundColor: "primary.main",
                  color: "white",
                  "& .MuiListItemIcon-root": { color: "white" },
                  "&:hover": {
                    backgroundColor: "primary.dark",
                  },
                },
                "&:hover": {
                  backgroundColor: "rgba(0, 51, 102, 0.08)",
                },
              }}
            >
              <ListItemIcon
                sx={{
                  minWidth: 40,
                  color:
                    location.pathname === item.path
                      ? "inherit"
                      : "primary.main",
                }}
              >
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={item.title}
                primaryTypographyProps={{
                  fontWeight: location.pathname === item.path ? 600 : 400,
                }}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Divider />

      {/* informatiile utilizatorului in partea de jos */}
      <Box sx={{ p: 2 }}>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            p: 1.5,
            borderRadius: 2,
            backgroundColor: "grey.100",
          }}
        >
          <Avatar sx={{ bgcolor: "primary.main", width: 36, height: 36 }}>
            {user?.firstName?.charAt(0)}
            {user?.lastName?.charAt(0)}
          </Avatar>
          <Box sx={{ overflow: "hidden" }}>
            <Typography
              variant="body2"
              sx={{ fontWeight: 600, lineHeight: 1.2 }}
              noWrap
            >
              {user?.fullName}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {user?.departmentName || user?.role}
            </Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      {/* AppBar */}
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          backgroundColor: "white",
          color: "text.primary",
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: "none" } }}
          >
            <MenuIcon />
          </IconButton>

          <Box sx={{ flexGrow: 1 }} />

          {/* notificari */}
          <IconButton color="inherit" sx={{ mr: 1 }} onClick={handleNotifOpen}>
            <Badge badgeContent={unreadCount} color="error">
              <NotificationIcon />
            </Badge>
          </IconButton>

          <Popover
            open={Boolean(notifAnchor)}
            anchorEl={notifAnchor}
            onClose={handleNotifClose}
            anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
            transformOrigin={{ vertical: "top", horizontal: "right" }}
            PaperProps={{ sx: { width: 360, maxHeight: 480 } }}
          >
            {/* header popover */}
            <Box
              sx={{
                px: 2,
                py: 1.5,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                borderBottom: "1px solid",
                borderColor: "divider",
              }}
            >
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                Notificări {unreadCount > 0 && `(${unreadCount} noi)`}
              </Typography>
              {unreadCount > 0 && (
                <Button
                  size="small"
                  startIcon={<DoneAllIcon fontSize="small" />}
                  onClick={handleMarkAllRead}
                >
                  Marchează toate
                </Button>
              )}
            </Box>

            {/* lista notificari */}
            <Box sx={{ overflowY: "auto", maxHeight: 380 }}>
              {notifications.length === 0 ? (
                <Box sx={{ p: 4, textAlign: "center" }}>
                  <NotificationIcon sx={{ fontSize: 40, color: "grey.300", mb: 1 }} />
                  <Typography variant="body2" color="text.secondary">
                    Nicio notificare
                  </Typography>
                </Box>
              ) : (
                notifications.map((notif) => (
                  <Box
                    key={notif.id}
                    onClick={() => !notif.isRead && handleMarkRead(notif.id)}
                    sx={{
                      px: 2,
                      py: 1.5,
                      borderBottom: "1px solid",
                      borderColor: "divider",
                      backgroundColor: notif.isRead ? "transparent" : "primary.50",
                      cursor: notif.isRead ? "default" : "pointer",
                      "&:hover": { backgroundColor: "grey.50" },
                      "&:last-child": { borderBottom: "none" },
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: 1 }}>
                      <Typography
                        variant="body2"
                        sx={{ fontWeight: notif.isRead ? 400 : 600 }}
                      >
                        {notif.subject}
                      </Typography>
                      {!notif.isRead && (
                        <Chip label="Nou" size="small" color="primary" sx={{ height: 18, fontSize: "0.65rem" }} />
                      )}
                    </Box>
                    <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.3 }}>
                      {notif.message}
                    </Typography>
                    <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 0.5 }}>
                      {new Date(notif.createdAt).toLocaleString("ro-RO")}
                    </Typography>
                  </Box>
                ))
              )}
            </Box>
          </Popover>

          {/* meniul utilizatorului */}
          <IconButton onClick={handleMenuOpen} color="inherit">
            <Avatar sx={{ bgcolor: "primary.main", width: 32, height: 32 }}>
              {user?.firstName?.charAt(0)}
            </Avatar>
          </IconButton>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
            transformOrigin={{ vertical: "top", horizontal: "right" }}
          >
            <MenuItem
              onClick={() => {
                handleMenuClose();
                navigate("/profile");
              }}
            >
              <ListItemIcon>
                <AccountIcon fontSize="small" />
              </ListItemIcon>
              Profil
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Deconectare
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* drawer-ul */}
      <Box
        component="nav"
        sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}
      >
        {/* drawer-ul pentru mobil */}
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", md: "none" },
            "& .MuiDrawer-paper": {
              boxSizing: "border-box",
              width: DRAWER_WIDTH,
            },
          }}
        >
          {drawer}
        </Drawer>

        {/* Desktop drawer */}
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", md: "block" },
            "& .MuiDrawer-paper": {
              boxSizing: "border-box",
              width: DRAWER_WIDTH,
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      {/* main content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: "64px",
          backgroundColor: "background.default",
          minHeight: "calc(100vh - 64px)",
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
};

export default MainLayout;
