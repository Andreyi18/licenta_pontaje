-- Script pentru generarea datelor de test pentru Secretariat Dashboard
-- Data: 18 Martie 2026

-- 1. Creează utilizatori profesori suplimentari dacă nu există
INSERT INTO users (email, password_hash, first_name, last_name, role, department_id, status)
SELECT 'elena.vasilescu@upt.ro', '$2b$12$XMND.1NtFNcb1fecKRFCdeWrz/RXYNBU5E2tVJKHdElLXf/nxzkRa', 'Elena', 'Vasilescu', 'CADRU_DIDACTIC', id, 'ACTIVE'
FROM departments WHERE code = 'CTI'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, first_name, last_name, role, department_id, status)
SELECT 'marius.georgescu@upt.ro', '$2b$12$XMND.1NtFNcb1fecKRFCdeWrz/RXYNBU5E2tVJKHdElLXf/nxzkRa', 'Marius', 'Georgescu', 'CADRU_DIDACTIC', id, 'ACTIVE'
FROM departments WHERE code = 'AIA'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, first_name, last_name, role, department_id, status)
SELECT 'daniela.marin@upt.ro', '$2b$12$XMND.1NtFNcb1fecKRFCdeWrz/RXYNBU5E2tVJKHdElLXf/nxzkRa', 'Daniela', 'Marin', 'CADRU_DIDACTIC', id, 'ACTIVE'
FROM departments WHERE code = 'ETC'
ON CONFLICT (email) DO NOTHING;

-- 2. Identificare ID-uri profesori
-- Notă: Folosim subquery-uri pentru a evita hardcoding-ul de UUID-uri

-- 3. Generare Pontaje pentru Martie 2026

-- Ion Ionescu (profesor@upt.ro) -> APPROVED
INSERT INTO timesheets (user_id, month, year, status, submitted_at)
SELECT id, 3, 2026, 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM users WHERE email = 'profesor@upt.ro'
ON CONFLICT (user_id, month, year) DO UPDATE SET status = 'APPROVED', submitted_at = CURRENT_TIMESTAMP - INTERVAL '2 days';

-- Elena Vasilescu -> SUBMITTED
INSERT INTO timesheets (user_id, month, year, status, submitted_at)
SELECT id, 3, 2026, 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM users WHERE email = 'elena.vasilescu@upt.ro'
ON CONFLICT (user_id, month, year) DO UPDATE SET status = 'SUBMITTED', submitted_at = CURRENT_TIMESTAMP - INTERVAL '1 day';

-- Marius Georgescu -> DRAFT
INSERT INTO timesheets (user_id, month, year, status)
SELECT id, 3, 2026, 'DRAFT'
FROM users WHERE email = 'marius.georgescu@upt.ro'
ON CONFLICT (user_id, month, year) DO UPDATE SET status = 'DRAFT';

-- 4. Adăugare intrări în pontaj pentru a avea ore de afișat

-- Intrări pentru Ion Ionescu (APPROVED)
INSERT INTO timesheet_entries (timesheet_id, entry_date, time_slot, hour_type, activity)
SELECT t.id, '2026-03-02', '08:00-10:00', 'NORMA', 'Curs Programare'
FROM timesheets t JOIN users u ON t.user_id = u.id 
WHERE u.email = 'profesor@upt.ro' AND t.month = 3 AND t.year = 2026;

INSERT INTO timesheet_entries (timesheet_id, entry_date, time_slot, hour_type, activity)
SELECT t.id, '2026-03-03', '14:00-16:00', 'PLATA_ORA', 'Consultatii Proiect'
FROM timesheets t JOIN users u ON t.user_id = u.id 
WHERE u.email = 'profesor@upt.ro' AND t.month = 3 AND t.year = 2026;

-- Intrări pentru Elena Vasilescu (SUBMITTED)
INSERT INTO timesheet_entries (timesheet_id, entry_date, time_slot, hour_type, activity)
SELECT t.id, '2026-03-04', '10:00-12:00', 'NORMA', 'Laborator Sisteme'
FROM timesheets t JOIN users u ON t.user_id = u.id 
WHERE u.email = 'elena.vasilescu@upt.ro' AND t.month = 3 AND t.year = 2026;

-- Mesaj final
DO $$
BEGIN
    RAISE NOTICE 'Datele de test pentru Martie 2026 au fost inserate!';
    RAISE NOTICE 'Distribuție statusuri:';
    RAISE NOTICE '  - Ion Ionescu: APPROVED (2h Norma + 2h Plata Ora)';
    RAISE NOTICE '  - Elena Vasilescu: SUBMITTED (2h Norma)';
    RAISE NOTICE '  - Marius Georgescu: DRAFT (0h momentan)';
    RAISE NOTICE '  - Daniela Marin: FĂRĂ PONTAJ (LIPSĂ)';
END $$;
