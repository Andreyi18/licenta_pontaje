-- Initializare baza de date UPT Pontaje
-- Script pentru PostgreSQL

-- Creare extensie UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabel departamente
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabel utilizatori
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CADRU_DIDACTIC', 'SECRETARIAT', 'ADMIN')),
    department_id UUID REFERENCES departments(id),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Tabel orar
CREATE TABLE IF NOT EXISTS schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('LUNI', 'MARTI', 'MIERCURI', 'JOI', 'VINERI', 'SAMBATA', 'DUMINICA')),
    time_slot VARCHAR(11) NOT NULL,
    discipline VARCHAR(200) NOT NULL,
    room VARCHAR(50),
    activity_type VARCHAR(15) NOT NULL CHECK (activity_type IN ('CURS', 'SEMINAR', 'LABORATOR', 'PROIECT')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schedules_user ON schedules(user_id);
CREATE INDEX idx_schedules_day ON schedules(day_of_week);

-- Tabel pontaje
CREATE TABLE IF NOT EXISTS timesheets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
    year INTEGER NOT NULL CHECK (year >= 2020),
    status VARCHAR(15) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED')),
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, month, year)
);

CREATE INDEX idx_timesheets_user ON timesheets(user_id);
CREATE INDEX idx_timesheets_period ON timesheets(month, year);
CREATE INDEX idx_timesheets_status ON timesheets(status);

-- Tabel intrari pontaj
CREATE TABLE IF NOT EXISTS timesheet_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timesheet_id UUID NOT NULL REFERENCES timesheets(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    time_slot VARCHAR(11) NOT NULL,
    hour_type VARCHAR(15) NOT NULL CHECK (hour_type IN ('NORMA', 'PLATA_ORA')),
    activity VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_entries_timesheet ON timesheet_entries(timesheet_id);
CREATE INDEX idx_entries_date ON timesheet_entries(entry_date);

-- Tabel documente
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    timesheet_id UUID NOT NULL REFERENCES timesheets(id) ON DELETE CASCADE,
    annex_type VARCHAR(10) NOT NULL CHECK (annex_type IN ('ANEXA_1', 'ANEXA_3')),
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documents_user ON documents(user_id);
CREATE INDEX idx_documents_timesheet ON documents(timesheet_id);

-- Tabel notificari
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(15) NOT NULL CHECK (notification_type IN ('REMINDER', 'SYSTEM', 'DEADLINE')),
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);

-- DATE INITIALE

-- Inserare departamente
INSERT INTO departments (name, code) VALUES
    ('Automatică și Informatică Aplicată', 'AIA'),
    ('Calculatoare și Tehnologia Informației', 'CTI'),
    ('Electronică și Telecomunicații', 'ETC'),
    ('Inginerie Electrică', 'IE'),
    ('Mecanică', 'MEC'),
    ('Arhitectură', 'ARH');

-- Inserare utilizator admin (parola: admin123)
-- BCrypt hash pentru 'admin123'
INSERT INTO users (email, password_hash, first_name, last_name, role, status) VALUES
    ('admin@upt.ro', '$2b$12$8ndsmUfctpsciCC6d5dbqeuTQGqVUUhPW7ycPIqLnFPsFwpxHppRi', 'Admin', 'UPT', 'ADMIN', 'ACTIVE');

-- Inserare utilizator secretariat de test (parola: secret123)
INSERT INTO users (email, password_hash, first_name, last_name, role, department_id, status)
SELECT 'secretariat@upt.ro', '$2b$12$/IpyZjCivgDxBssY/liCo.2nQuNIMLej40Eh9yNUnO0u5Aqk7VS6y', 'Ana', 'Popescu', 'SECRETARIAT', id, 'ACTIVE'
FROM departments WHERE code = 'CTI';

-- Inserare cadru didactic de test (parola: test123)
INSERT INTO users (email, password_hash, first_name, last_name, role, department_id, status)
SELECT 'profesor@upt.ro', '$2b$12$XMND.1NtFNcb1fecKRFCdeWrz/RXYNBU5E2tVJKHdElLXf/nxzkRa', 'Ion', 'Ionescu', 'CADRU_DIDACTIC', id, 'ACTIVE'
FROM departments WHERE code = 'CTI';

-- Afisare mesaj de succes
DO $$
BEGIN
    RAISE NOTICE 'Baza de date a fost initializata cu succes!';
    RAISE NOTICE 'Utilizatori de test:';
    RAISE NOTICE '  - admin@upt.ro / admin123 (Admin)';
    RAISE NOTICE '  - secretariat@upt.ro / secret123 (Secretariat)';
    RAISE NOTICE '  - profesor@upt.ro / test123 (Cadru Didactic)';
END $$;
