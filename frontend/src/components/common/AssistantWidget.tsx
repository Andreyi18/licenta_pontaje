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

// ----- tipuri interne -----
interface QuickAction {
  label: string;
  path?: string;
  reply?: string;
}

interface ChatMessage {
  id: number;
  from: "bot" | "user";
  text: string;
  actions?: QuickAction[];
}

// ----- baza de cunoștințe (reguli de intenție) -----
interface Intent {
  keywords: string[];
  roles?: UserRole[];
  answer: string;
  actions?: QuickAction[];
}

const buildIntents = (role?: UserRole): Intent[] => [
  {
    keywords: ["orar", "program", "discipl", "activit", "curs", "laborator"],
    roles: [UserRole.CADRU_DIDACTIC],
    answer:
      "În secțiunea Orar îți poți adăuga și gestiona activitățile săptămânale (curs, laborator, seminar). Disciplina este obligatorie, iar un interval ocupat în aceeași zi este blocat automat.",
    actions: [{ label: "Deschide Orarul", path: "/schedule" }],
  },
  {
    keywords: ["ponta", "ore", "oră", "trimit", "lună", "luna", "raport propriu"],
    roles: [UserRole.CADRU_DIDACTIC],
    answer:
      "În Pontaj înregistrezi orele lucrate pe zile. Apeși pe o zi, alegi intervalul și tipul orei, apoi adaugi. La final trimiți pontajul lunii spre secretariat pentru aprobare.",
    actions: [{ label: "Deschide Pontajul", path: "/timesheet" }],
  },
  {
    keywords: ["document", "pdf", "descarc", "generaz", "fișier", "fisier"],
    roles: [UserRole.CADRU_DIDACTIC],
    answer:
      "În Documente poți genera și descărca pontajul lunar în format PDF, gata de semnat.",
    actions: [{ label: "Deschide Documente", path: "/documents" }],
  },
  {
    keywords: ["centraliz", "aprob", "secretar", "verific", "în așteptare", "asteptare"],
    roles: [UserRole.SECRETARIAT, UserRole.ADMIN],
    answer:
      "În Centralizator vezi toate pontajele trimise de cadrele didactice. Le poți deschide, verifica și aproba, apoi genera raportul centralizat pentru concatenare.",
    actions: [{ label: "Deschide Centralizatorul", path: "/secretariat" }],
  },
  {
    keywords: ["raport", "trimit raport", "centraliz", "concaten", "export"],
    roles: [UserRole.SECRETARIAT, UserRole.ADMIN],
    answer:
      "Raportul centralizat adună pontajele aprobate într-un singur document. Din Centralizator apeși pe acțiunea de concatenare pentru a genera PDF-ul final pe care îl poți trimite mai departe.",
    actions: [{ label: "Mergi la Centralizator", path: "/secretariat" }],
  },
  {
    keywords: ["utilizator", "cont", "user", "adaug persoan", "rol", "parol nou"],
    roles: [UserRole.ADMIN],
    answer:
      "Din Utilizatori poți adăuga, edita sau șterge conturi și le poți seta rolul. Câmpurile obligatorii sunt marcate, iar butonul de salvare rămâne blocat până le completezi.",
    actions: [{ label: "Deschide Utilizatori", path: "/admin/users" }],
  },
  {
    keywords: ["profil", "parol", "schimb parol", "email", "date personale"],
    answer:
      "În Profil îți poți actualiza datele personale și schimba parola. Pentru o parolă nouă trebuie să introduci și parola curentă.",
    actions: [{ label: "Deschide Profilul", path: "/profile" }],
  },
  {
    keywords: ["salut", "buna", "bună", "hei", "hello", "noroc", "servus"],
    answer: "Salut! Cu ce te pot ajuta astăzi?",
  },
  {
    keywords: ["mulțum", "multum", "mersi", "thanks", "merci"],
    answer: "Cu plăcere! Dacă mai ai nevoie de ceva, sunt aici. 🙂",
  },
  {
    keywords: ["ajutor", "help", "ce poti", "ce poți", "cum", "nu stiu", "nu știu"],
    answer:
      role === UserRole.SECRETARIAT || role === UserRole.ADMIN
        ? "Te pot ghida prin: Centralizator (verificare/aprobare pontaje), generare raport centralizat și gestionare utilizatori. Alege o acțiune rapidă mai jos."
        : "Te pot ghida prin: Orar, Pontaj și Documente. Spune-mi ce vrei să faci sau alege o acțiune rapidă mai jos.",
  },
];

// sugestii rapide în funcție de rol
const quickSuggestions = (role?: UserRole): QuickAction[] => {
  if (role === UserRole.SECRETARIAT) {
    return [
      { label: "Pontaje în așteptare", path: "/secretariat" },
      { label: "Cum trimit raportul?", reply: "raport" },
      { label: "Ajutor", reply: "ajutor" },
    ];
  }
  if (role === UserRole.ADMIN) {
    return [
      { label: "Centralizator", path: "/secretariat" },
      { label: "Utilizatori", path: "/admin/users" },
      { label: "Ajutor", reply: "ajutor" },
    ];
  }
  return [
    { label: "Adaugă pontaj", path: "/timesheet" },
    { label: "Vezi orarul", path: "/schedule" },
    { label: "Generează document", path: "/documents" },
    { label: "Ajutor", reply: "ajutor" },
  ];
};

const normalize = (s: string) =>
  s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "");

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
  const typingTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const intents = useMemo(() => buildIntents(role), [role]);
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
          text: `Bună${name}! Sunt asistentul Pontaje UPT. Te pot ajuta să navighezi rapid și să-ți explic funcțiile aplicației. Cu ce începem?`,
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

  useEffect(
    () => () => {
      if (typingTimer.current) clearTimeout(typingTimer.current);
    },
    [],
  );

  const findIntent = (text: string): Intent | null => {
    const n = normalize(text);
    let best: { intent: Intent; score: number } | null = null;
    for (const intent of intents) {
      if (intent.roles && role && !intent.roles.includes(role)) continue;
      let score = 0;
      for (const kw of intent.keywords) {
        if (n.includes(normalize(kw))) score += kw.length;
      }
      if (score > 0 && (!best || score > best.score)) best = { intent, score };
    }
    return best?.intent ?? null;
  };

  const pushBotReply = (text: string) => {
    const intent = findIntent(text);
    const reply: ChatMessage = intent
      ? {
          id: nextId(),
          from: "bot",
          text: intent.answer,
          actions: intent.actions,
        }
      : {
          id: nextId(),
          from: "bot",
          text: "Momentan pot răspunde la întrebări despre orar, pontaj, documente, rapoarte și cont. Încearcă una dintre acțiunile rapide de mai jos.",
          actions: suggestions,
        };

    setIsTyping(true);
    typingTimer.current = setTimeout(() => {
      setIsTyping(false);
      setMessages((prev) => [...prev, reply]);
    }, 550);
  };

  const sendUserMessage = (text: string) => {
    const clean = text.trim();
    if (!clean) return;
    setMessages((prev) => [
      ...prev,
      { id: nextId(), from: "user", text: clean },
    ]);
    setInput("");
    pushBotReply(clean);
  };

  const handleAction = (action: QuickAction) => {
    if (action.path) {
      setMessages((prev) => [
        ...prev,
        { id: nextId(), from: "user", text: action.label },
        {
          id: nextId(),
          from: "bot",
          text: `Te duc la „${action.label}”…`,
        },
      ]);
      navigate(action.path);
      setOpen(false);
      return;
    }
    if (action.reply) {
      setMessages((prev) => [
        ...prev,
        { id: nextId(), from: "user", text: action.label },
      ]);
      pushBotReply(action.reply);
    }
  };

  return (
    <>
      {/* buton flotant */}
      {!open && (
        <Tooltip title="Asistent Pontaje" placement="left" arrow>
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
                Asistent Pontaje
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
                  Online · răspunde instant
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
              backgroundColor: "#f4f6fb",
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
                        msg.from === "user" ? "primary.main" : "#ffffff",
                      color: msg.from === "user" ? "white" : "text.primary",
                      boxShadow:
                        msg.from === "user"
                          ? "0 4px 12px rgba(0,51,102,0.22)"
                          : "0 2px 8px rgba(15,35,65,0.07)",
                      border:
                        msg.from === "bot"
                          ? "1px solid rgba(15,35,65,0.06)"
                          : "none",
                    }}
                  >
                    <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                      {msg.text}
                    </Typography>
                  </Box>
                </Box>

                {/* acțiuni atașate mesajului botului */}
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
                          backgroundColor: "white",
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
                    backgroundColor: "#ffffff",
                    border: "1px solid rgba(15,35,65,0.06)",
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
              backgroundColor: "white",
            }}
          >
            <TextField
              fullWidth
              size="small"
              placeholder="Scrie un mesaj…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              autoComplete="off"
              sx={{
                "& .MuiOutlinedInput-root": { borderRadius: 6 },
              }}
            />
            <IconButton
              type="submit"
              color="primary"
              disabled={!input.trim()}
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
              backgroundColor: "white",
              borderTop: "1px solid rgba(15,35,65,0.05)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 0.5,
            }}
          >
            <SparkleIcon sx={{ fontSize: 12, color: "text.disabled" }} />
            <Typography variant="caption" color="text.disabled">
              Asistent inteligent · Pontaje UPT
            </Typography>
          </Box>
        </Paper>
      </Slide>
    </>
  );
};

export default AssistantWidget;
