import { createTheme } from "@mui/material/styles";
import type { ThemeOptions } from "@mui/material/styles";

// upt design system colors
const uptColors = {
  primary: {
    main: "#003366", // Corporate Blue
    light: "#0066cc", // Accent Blue
    dark: "#002244", // Deep Blue
    contrastText: "#ffffff",
  },
  secondary: {
    main: "#0066cc", // Accent Blue
    light: "#3399ff", // Light Blue
    dark: "#004d99", // Darker Blue
    contrastText: "#ffffff",
  },
  background: {
    default: "#f5f7fa", // Light Gray Background
    paper: "#ffffff",
  },
  text: {
    primary: "#1a1a1a",
    secondary: "#666666",
  },
  success: {
    main: "#28a745",
    light: "#48c764",
    dark: "#1e7e34",
  },
  warning: {
    main: "#ffc107",
    light: "#ffcd38",
    dark: "#e0a800",
  },
  error: {
    main: "#dc3545",
    light: "#e4606d",
    dark: "#bd2130",
  },
  info: {
    main: "#17a2b8",
    light: "#3eb8cc",
    dark: "#117a8b",
  },
};

// Theme configuration
const themeOptions: ThemeOptions = {
  palette: {
    mode: "light",
    primary: uptColors.primary,
    secondary: uptColors.secondary,
    background: uptColors.background,
    text: uptColors.text,
    success: uptColors.success,
    warning: uptColors.warning,
    error: uptColors.error,
    info: uptColors.info,
  },
  typography: {
    fontFamily: "'Inter', 'Roboto', 'Helvetica', 'Arial', sans-serif",
    h1: {
      fontSize: "2.5rem",
      fontWeight: 700,
      letterSpacing: "-0.02em",
    },
    h2: {
      fontSize: "2rem",
      fontWeight: 600,
      letterSpacing: "-0.01em",
    },
    h3: {
      fontSize: "1.5rem",
      fontWeight: 600,
    },
    h4: {
      fontSize: "1.25rem",
      fontWeight: 600,
    },
    h5: {
      fontSize: "1.1rem",
      fontWeight: 500,
    },
    h6: {
      fontSize: "1rem",
      fontWeight: 500,
    },
    body1: {
      fontSize: "1rem",
      lineHeight: 1.6,
    },
    body2: {
      fontSize: "0.875rem",
      lineHeight: 1.5,
    },
    button: {
      textTransform: "none",
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 8,
  },
  shadows: [
    "none",
    "0px 2px 4px rgba(0, 0, 0, 0.05)",
    "0px 4px 8px rgba(0, 0, 0, 0.08)",
    "0px 8px 16px rgba(0, 0, 0, 0.10)",
    "0px 12px 24px rgba(0, 0, 0, 0.12)",
    "0px 16px 32px rgba(0, 0, 0, 0.14)",
    "0px 20px 40px rgba(0, 0, 0, 0.16)",
    "0px 24px 48px rgba(0, 0, 0, 0.18)",
    "0px 28px 56px rgba(0, 0, 0, 0.20)",
    "0px 32px 64px rgba(0, 0, 0, 0.22)",
    "0px 36px 72px rgba(0, 0, 0, 0.24)",
    "0px 40px 80px rgba(0, 0, 0, 0.26)",
    "0px 44px 88px rgba(0, 0, 0, 0.28)",
    "0px 48px 96px rgba(0, 0, 0, 0.30)",
    "0px 52px 104px rgba(0, 0, 0, 0.32)",
    "0px 56px 112px rgba(0, 0, 0, 0.34)",
    "0px 60px 120px rgba(0, 0, 0, 0.36)",
    "0px 64px 128px rgba(0, 0, 0, 0.38)",
    "0px 68px 136px rgba(0, 0, 0, 0.40)",
    "0px 72px 144px rgba(0, 0, 0, 0.42)",
    "0px 76px 152px rgba(0, 0, 0, 0.44)",
    "0px 80px 160px rgba(0, 0, 0, 0.46)",
    "0px 84px 168px rgba(0, 0, 0, 0.48)",
    "0px 88px 176px rgba(0, 0, 0, 0.50)",
    "0px 92px 184px rgba(0, 0, 0, 0.52)",
  ],
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: uptColors.background.default,
          scrollbarWidth: "thin",
          "&::-webkit-scrollbar": {
            width: "8px",
            height: "8px",
          },
          "&::-webkit-scrollbar-track": {
            background: "#f1f1f1",
          },
          "&::-webkit-scrollbar-thumb": {
            background: "#c1c1c1",
            borderRadius: "4px",
          },
          "&::-webkit-scrollbar-thumb:hover": {
            background: "#999",
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: "10px 24px",
          fontSize: "0.9rem",
          boxShadow: "none",
          "&:hover": {
            boxShadow: "0px 4px 12px rgba(0, 51, 102, 0.2)",
          },
        },
        contained: {
          "&:hover": {
            boxShadow: "0px 6px 16px rgba(0, 51, 102, 0.25)",
          },
        },
        outlined: {
          borderWidth: 2,
          "&:hover": {
            borderWidth: 2,
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: "0px 4px 16px rgba(0, 0, 0, 0.08)",
          border: "1px solid rgba(0, 0, 0, 0.06)",
          transition: "box-shadow 0.3s ease, transform 0.2s ease",
          "&:hover": {
            boxShadow: "0px 8px 24px rgba(0, 0, 0, 0.12)",
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
        elevation1: {
          boxShadow: "0px 2px 8px rgba(0, 0, 0, 0.06)",
        },
        elevation2: {
          boxShadow: "0px 4px 16px rgba(0, 0, 0, 0.08)",
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: "0px 2px 8px rgba(0, 0, 0, 0.08)",
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRight: "none",
          boxShadow: "2px 0px 8px rgba(0, 0, 0, 0.08)",
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          "& .MuiOutlinedInput-root": {
            borderRadius: 8,
            "&:hover .MuiOutlinedInput-notchedOutline": {
              borderColor: uptColors.primary.light,
            },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
              borderWidth: 2,
            },
          },
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor: uptColors.background.default,
          "& .MuiTableCell-head": {
            fontWeight: 600,
            color: uptColors.text.primary,
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          "&:hover": {
            backgroundColor: "rgba(0, 102, 204, 0.04)",
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          fontWeight: 500,
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
  },
};

// Create theme
export const theme = createTheme(themeOptions);

// Dark theme variant
export const darkTheme = createTheme({
  ...themeOptions,
  palette: {
    mode: "dark",
    primary: uptColors.primary,
    secondary: uptColors.secondary,
    background: {
      default: "#121212",
      paper: "#1e1e1e",
    },
    text: {
      primary: "#ffffff",
      secondary: "#b0b0b0",
    },
    success: uptColors.success,
    warning: uptColors.warning,
    error: uptColors.error,
    info: uptColors.info,
  },
});

export default theme;
