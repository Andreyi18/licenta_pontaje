import React, { useState, useRef, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Paper,
  IconButton,
  Typography,
  Avatar,
  TextField,
  Chip,
  Fab,
  Slide,
  Tooltip,
  Divider,
} from "@mui/material";
import {
  SmartToy as BotIcon,
  Close as CloseIcon,
  Send as SendIcon,
  AutoAwesome as SparkleIcon,
} from "@mui/icons-material";
import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types";
import { assistantApi, type AssistantMessage } from "../../api/api";

// ----- tipuri interne -----
interface QuickAction {
  label: string;
  path?: string; // navighează în aplicație
  prompt?: string; // trimite o întrebare către AI
}

interface ChatMessage {
  id: number;
  from: "bot" | "user";
  text: string;
  actions?: QuickAction[];
}

// sugestii rapide (scurtături) în funcție de rol — navigare + întrebări către AI
const quickSuggestions = (role?: UserRole): QuickAction[] => {
  if (role === UserRole.SECRETARIAT) {
    return [
      { label: "Pontaje în așteptare", path: "/secretariat" },
      { label: "Cum trimit raportul pe email?", prompt: "Cum trimit raportul centralizat pe email?" },
      { label: "Ce poți face?", prompt: "Ce poți face și cu ce mă poți ajuta?" },
    ];
  }
  if (role === UserRole.ADMIN) {
    return [
      { label: "Centralizator", path: "/secretariat" },
      { label: "Utilizatori", path: "/admin/users" },
      { label: "Ce poți face?", prompt: "Ce poți face și cu ce mă poți ajuta?" },
    ];
  }
  return [
    { label: "Adaugă pontaj", path: "/timesheet" },
    { label: "Vezi orarul", path: "/schedule" },
    { label: "Cum generez Anexa 1?", prompt: "Cum generez și descarc Anexa 1?" },
    { label: "Ce poți face?", prompt: "Ce poți face și cu ce mă poți ajuta?" },
  ];
};

const AssistantWidget: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const role = user?.role as UserRole | undefined;

  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const idRef = useRef(0);
  const endRef = useRef<HTMLDivElement>(null);

  const suggestions = useMemo(() => quickSuggestions(role), [role]);

  const nextId = () => ++idRef.current;

  // mesaj de întâmpinare la prima deschidere
  useEffect(() => {
    if (open && messages.length === 0) {
      const name = user?.firstName ? `, ${user.firstName}` : "";
      setMessages([
        {
          id: nextId(),
          from: "bot",
          text: `Bună${name}! Sunt asistentul AI Pontaje UPT. Întreabă-mă orice despre aplicație — orar, pontaj, documente sau rapoarte.`,
          actions: suggestions,
        },
      ]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // auto-scroll
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  // construiește istoricul pentru API din mesajele afișate
  const buildHistory = (msgs: ChatMessage[]): AssistantMessage[] =>
    msgs
      .filter((m) => m.text && m.text.trim() !== "")
      .map((m) => ({
        role: m.from === "user" ? "user" : "assistant",
        content: m.text,
      }));

  const askAI = async (updatedMessages: ChatMessage[]) => {
    setIsTyping(true);
    const botId = nextId();
    const history = buildHistory(updatedMessages);
    let started = false;
    let acc = "";
    try {
      await assistantApi.chatStream(history, (chunk) => {
        acc += chunk;
        if (!started) {
          started = true;
          setIsTyping(false);
          setMessages((prev) => [...prev, { id: botId, from: "bot", text: acc }]);
        } else {
          setMessages((prev) =>
            prev.map((m) => (m.id === botId ? { ...m, text: acc } : m)),
          );
        }
      });
    } catch {
      // Streaming-ul a întâmpinat o eroare.
      if (started) {
        // Tokenii au sosit deja → păstrăm răspunsul, fără banner de eroare.
      } else {
        // Streaming indisponibil → fallback transparent la varianta non-streaming.
        try {
          const res = await assistantApi.chat(history);
          started = true;
          setMessages((prev) => [
            ...prev,
            { id: botId, from: "bot", text: res.reply },
          ]);
        } catch (e: any) {
          setMessages((prev) => [
            ...prev,
            {
              id: botId,
              from: "bot",
              text:
                e?.message ||
                "Asistentul nu a putut răspunde momentan. Încearcă din nou.",
            },
          ]);
        }
      }
    } finally {
      setIsTyping(false);
    }
  };

  const sendUserMessage = (text: string) => {
    const clean = text.trim();
    if (!clean || isTyping) return;
    const userMsg: ChatMessage = { id: nextId(), from: "user", text: clean };
    const updated = [...messages, userMsg];
    setMessages(updated);
    setInput("");
    askAI(updated);
  };

  const handleAction = (action: QuickAction) => {
    if (action.path) {
      navigate(action.path);
      setOpen(false);
      return;
    }
    if (action.prompt) {
      sendUserMessage(action.prompt);
    }
  };

  return (
    <>
      {/* buton flotant */}
      {!open && (
        <Tooltip title="Asistent AI" placement="left" arrow>
          <Fab
            color="primary"
            onClick={() => setOpen(true)}
            aria-label="Deschide asistentul"
            sx={{
              position: "fixed",
              bottom: { xs: 16, md: 24 },
              right: { xs: 16, md: 24 },
              zIndex: 1300,
              background: "linear-gradient(135deg, #003366 0%, #0066cc 100%)",
              boxShadow: "0 10px 24px rgba(0, 51, 102, 0.4)",
              "&:hover": {
                background: "linear-gradient(135deg, #002244 0%, #004d99 100%)",
              },
              "&::after": {
                content: '""',
                position: "absolute",
                inset: 0,
                borderRadius: "50%",
                border: "2px solid rgba(0, 102, 204, 0.5)",
                animation: "assistantPulse 2.2s ease-out infinite",
              },
              "@keyframes assistantPulse": {
                "0%": { transform: "scale(1)", opacity: 0.7 },
                "100%": { transform: "scale(1.6)", opacity: 0 },
              },
            }}
          >
            <BotIcon />
          </Fab>
        </Tooltip>
      )}

      {/* panou chat */}
      <Slide direction="up" in={open} mountOnEnter unmountOnExit>
        <Paper
          elevation={8}
          sx={{
            position: "fixed",
            bottom: { xs: 0, md: 24 },
            right: { xs: 0, md: 24 },
            width: { xs: "100%", sm: 380 },
            height: { xs: "85vh", sm: 560 },
            maxHeight: "85vh",
            display: "flex",
            flexDirection: "column",
            borderRadius: { xs: "16px 16px 0 0", sm: 4 },
            overflow: "hidden",
            zIndex: 1300,
          }}
        >
          {/* header */}
          <Box
            sx={{
              px: 2,
              py: 1.5,
              display: "flex",
              alignItems: "center",
              gap: 1.5,
              background: "linear-gradient(135deg, #003366 0%, #0066cc 100%)",
              color: "white",
            }}
          >
            <Avatar
              sx={{
                bgcolor: "rgba(255,255,255,0.18)",
                width: 38,
                height: 38,
              }}
            >
              <BotIcon fontSize="small" />
            </Avatar>
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.1 }}>
                Asistent AI
              </Typography>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    borderRadius: "50%",
                    bgcolor: "#4ade80",
                  }}
                />
                <Typography variant="caption" sx={{ opacity: 0.85 }}>
                  Online · powered by Groq
                </Typography>
              </Box>
            </Box>
            <IconButton
              size="small"
              onClick={() => setOpen(false)}
              sx={{ color: "white" }}
              aria-label="Închide asistentul"
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* mesaje */}
          <Box
            sx={{
              flexGrow: 1,
              overflowY: "auto",
              px: 1.5,
              py: 2,
              backgroundColor: "background.default",
              display: "flex",
              flexDirection: "column",
              gap: 1.25,
            }}
          >
            {messages.map((msg) => (
              <Box key={msg.id}>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent:
                      msg.from === "user" ? "flex-end" : "flex-start",
                  }}
                >
                  <Box
                    sx={{
                      maxWidth: "82%",
                      px: 1.75,
                      py: 1.25,
                      borderRadius:
                        msg.from === "user"
                          ? "14px 14px 4px 14px"
                          : "14px 14px 14px 4px",
                      backgroundColor:
                        msg.from === "user" ? "primary.main" : "background.paper",
                      color: msg.from === "user" ? "white" : "text.primary",
                      boxShadow:
                        msg.from === "user"
                          ? "0 4px 12px rgba(0,51,102,0.22)"
                          : "0 2px 8px rgba(15,35,65,0.12)",
                      border:
                        msg.from === "bot"
                          ? "1px solid rgba(127,127,127,0.18)"
                          : "none",
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                      {msg.text}
                    </Typography>
                  </Box>
                </Box>

                {/* scurtături atașate mesajului botului */}
                {msg.actions && msg.actions.length > 0 && (
                  <Box
                    sx={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: 0.75,
                      mt: 1,
                      ml: 0.5,
                    }}
                  >
                    {msg.actions.map((a) => (
                      <Chip
                        key={a.label}
                        label={a.label}
                        size="small"
                        onClick={() => handleAction(a)}
                        clickable
                        variant="outlined"
                        sx={{
                          borderColor: "primary.light",
                          color: "primary.main",
                          backgroundColor: "background.paper",
                          "&:hover": {
                            backgroundColor: "primary.main",
                            color: "white",
                          },
                        }}
                      />
                    ))}
                  </Box>
                )}
              </Box>
            ))}

            {/* indicator typing */}
            {isTyping && (
              <Box sx={{ display: "flex", justifyContent: "flex-start" }}>
                <Box
                  sx={{
                    px: 1.75,
                    py: 1.5,
                    borderRadius: "14px 14px 14px 4px",
                    backgroundColor: "background.paper",
                    border: "1px solid rgba(127,127,127,0.18)",
                    display: "flex",
                    gap: 0.5,
                  }}
                >
                  {[0, 1, 2].map((i) => (
                    <Box
                      key={i}
                      sx={{
                        width: 7,
                        height: 7,
                        borderRadius: "50%",
                        backgroundColor: "primary.light",
                        animation: "assistantTyping 1.2s infinite ease-in-out",
                        animationDelay: `${i * 0.18}s`,
                        "@keyframes assistantTyping": {
                          "0%, 60%, 100%": { transform: "translateY(0)", opacity: 0.4 },
                          "30%": { transform: "translateY(-4px)", opacity: 1 },
                        },
                      }}
                    />
                  ))}
                </Box>
              </Box>
            )}
            <div ref={endRef} />
          </Box>

          <Divider />

          {/* input */}
          <Box
            component="form"
            onSubmit={(e) => {
              e.preventDefault();
              sendUserMessage(input);
            }}
            sx={{
              p: 1.25,
              display: "flex",
              alignItems: "center",
              gap: 1,
              backgroundColor: "background.paper",
            }}
          >
            <TextField
              fullWidth
              size="small"
              placeholder="Scrie un mesaj…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              autoComplete="off"
              disabled={isTyping}
              sx={{
                "& .MuiOutlinedInput-root": { borderRadius: 6 },
              }}
            />
            <IconButton
              type="submit"
              color="primary"
              disabled={!input.trim() || isTyping}
              aria-label="Trimite mesaj"
              sx={{
                background: input.trim()
                  ? "linear-gradient(135deg, #003366 0%, #0066cc 100%)"
                  : "transparent",
                color: input.trim() ? "white" : "action.disabled",
                "&:hover": {
                  background: input.trim()
                    ? "linear-gradient(135deg, #002244 0%, #004d99 100%)"
                    : "transparent",
                },
              }}
            >
              <SendIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* footer mic */}
          <Box
            sx={{
              px: 2,
              py: 0.5,
              backgroundColor: "background.paper",
              borderTop: "1px solid rgba(127,127,127,0.12)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 0.5,
            }}
          >
            <SparkleIcon sx={{ fontSize: 12, color: "text.disabled" }} />
            <Typography variant="caption" color="text.disabled">
              Asistent AI · Pontaje UPT
            </Typography>
          </Box>
        </Paper>
      </Slide>
    </>
  );
};

export default AssistantWidget;
