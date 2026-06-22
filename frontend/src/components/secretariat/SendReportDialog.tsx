import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  Chip,
  CircularProgress,
  InputAdornment,
  IconButton,
} from "@mui/material";
import {
  Send as SendIcon,
  Email as EmailIcon,
  PictureAsPdf as PdfIcon,
  Close as CloseIcon,
} from "@mui/icons-material";
import toast from "react-hot-toast";
import { secretariatApi } from "../../api/api";
import { MONTHS } from "../../types";

interface SendReportDialogProps {
  open: boolean;
  month: number;
  year: number;
  onClose: () => void;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// transformă text liber în listă de emailuri (separate prin virgulă, punct-virgulă sau spațiu)
const parseEmails = (raw: string): string[] =>
  raw
    .split(/[,;\s]+/)
    .map((e) => e.trim())
    .filter(Boolean);

const SendReportDialog: React.FC<SendReportDialogProps> = ({
  open,
  month,
  year,
  onClose,
}) => {
  const monthName = MONTHS[month - 1] ?? String(month);

  const [to, setTo] = useState("");
  const [cc, setCc] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [sending, setSending] = useState(false);
  const [emailConfigured, setEmailConfigured] = useState<boolean | null>(null);

  // la deschidere: precompletează subiect/corp și verifică configurarea email
  useEffect(() => {
    if (!open) return;
    setSubject(`Raport pontaje ${monthName} ${year}`);
    setBody(
      `Bună ziua,\n\nAtașat regăsiți raportul centralizat al pontajelor pentru luna ${monthName} ${year}.\n\nNumeroase mulțumiri,\nSecretariat`,
    );
    secretariatApi
      .getEmailConfig()
      .then((r) => {
        setEmailConfigured(r.configured);
        // precompletează destinatarul implicit configurat pe server (sau golește)
        setTo(r.defaultRecipient || "");
      })
      .catch(() => setEmailConfigured(null));
  }, [open, monthName, year]);

  const toList = parseEmails(to);
  const ccList = parseEmails(cc);
  const invalidTo = toList.filter((e) => !EMAIL_RE.test(e));
  const invalidCc = ccList.filter((e) => !EMAIL_RE.test(e));

  const canSend =
    toList.length > 0 &&
    invalidTo.length === 0 &&
    invalidCc.length === 0 &&
    subject.trim() !== "" &&
    !sending;

  const handleSend = async () => {
    if (toList.length === 0) {
      toast.error("Adăugați cel puțin un destinatar");
      return;
    }
    if (invalidTo.length > 0 || invalidCc.length > 0) {
      toast.error("Verificați adresele de email — unele sunt invalide");
      return;
    }
    setSending(true);
    try {
      const res = await secretariatApi.sendReport(month, year, {
        to: toList,
        cc: ccList.length > 0 ? ccList : undefined,
        subject: subject.trim(),
        body: body.trim(),
      });
      toast.success(res.message || "Raport trimis cu succes");
      onClose();
    } catch (err: any) {
      toast.error(err.message || "Eroare la trimiterea raportului");
    } finally {
      setSending(false);
    }
  };

  return (
    <Dialog open={open} onClose={sending ? undefined : onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <EmailIcon color="primary" />
        Trimite raportul pe email
        <IconButton
          aria-label="Închide"
          onClick={onClose}
          disabled={sending}
          sx={{ ml: "auto", color: "text.secondary" }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers>
        {/* perioada + atașament */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            mb: 2,
            p: 1.5,
            borderRadius: 2,
            backgroundColor: "grey.100",
          }}
        >
          <PdfIcon color="error" />
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              pontaje_consolidate_{monthName.toLowerCase()}_{year}.pdf
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Raportul centralizat pentru {monthName} {year} va fi atașat automat.
            </Typography>
          </Box>
        </Box>

        {emailConfigured === false && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Serviciul de email nu este configurat pe server (MAIL_USERNAME /
            MAIL_PASSWORD). Trimiterea va eșua până la configurare.
          </Alert>
        )}

        <TextField
          fullWidth
          label="Destinatari (To)"
          placeholder="ex: decanat@upt.ro, director@upt.ro"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          required
          sx={{ mb: 1 }}
          error={invalidTo.length > 0}
          helperText={
            invalidTo.length > 0
              ? `Adrese invalide: ${invalidTo.join(", ")}`
              : "Separă mai multe adrese prin virgulă"
          }
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <EmailIcon fontSize="small" color="action" />
              </InputAdornment>
            ),
          }}
        />
        {toList.length > 0 && invalidTo.length === 0 && (
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5, mb: 2 }}>
            {toList.map((e) => (
              <Chip key={e} label={e} size="small" color="primary" variant="outlined" />
            ))}
          </Box>
        )}

        <TextField
          fullWidth
          label="Copie (Cc) — opțional"
          placeholder="ex: arhiva@upt.ro"
          value={cc}
          onChange={(e) => setCc(e.target.value)}
          sx={{ mb: 2, mt: 1 }}
          error={invalidCc.length > 0}
          helperText={
            invalidCc.length > 0 ? `Adrese invalide: ${invalidCc.join(", ")}` : " "
          }
        />

        <TextField
          fullWidth
          label="Subiect"
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
          required
          sx={{ mb: 2 }}
          error={subject.trim() === ""}
        />

        <TextField
          fullWidth
          label="Mesaj"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          multiline
          minRows={5}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={sending}>
          Anulează
        </Button>
        <Button
          variant="contained"
          onClick={handleSend}
          disabled={!canSend}
          startIcon={
            sending ? <CircularProgress size={16} color="inherit" /> : <SendIcon />
          }
        >
          {sending ? "Se trimite…" : "Trimite"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SendReportDialog;
