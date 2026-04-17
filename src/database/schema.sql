-- Create a database for the VCRTS project for Milestone 6
CREATE DATABASE IF NOT EXISTS vcrts_db;
USE vcrts_db;

-- Users table (replaces users.txt)
CREATE TABLE IF NOT EXISTS users (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        ENUM('CLIENT', 'OWNER', 'ADMIN') NOT NULL DEFAULT 'CLIENT',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Table for client job submissions (ROLE: CLIENT)
-- job_id is the user-provided Client ID (matches existing Job model behavior)
CREATE TABLE IF NOT EXISTS jobs (
    job_id          INT PRIMARY KEY,
    description     VARCHAR(255) NOT NULL,
    duration_hours  INT NOT NULL,
    arrival_time    DATETIME,
    deadline_time   DATETIME,
    jobStatus       ENUM('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED') DEFAULT 'QUEUED',
    completionTime  INT
);

-- Table for vehicle owner submissions (ROLE: OWNER)
-- owner_id is the user-provided Owner ID from the form (not FK to users.id)
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id            INT PRIMARY KEY AUTO_INCREMENT,
    owner_id              INT,
    vehicle_info          VARCHAR(255) NOT NULL,
    residency_hours       INT NOT NULL,
    vehicle_status        VARCHAR(50) DEFAULT 'IDLE',
    vehicle_availability  BOOLEAN DEFAULT TRUE,
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Table for logs (ROLE: ADMIN)
CREATE TABLE IF NOT EXISTS logs (
    log_id         INT PRIMARY KEY AUTO_INCREMENT,
    log_message    VARCHAR(255) NOT NULL,
    log_timestamp  DATETIME NOT NULL
);

-- Table for notifications (ROLE: ADMIN)
CREATE TABLE IF NOT EXISTS notifications (
    notification_id         INT PRIMARY KEY AUTO_INCREMENT,
    notification_message    VARCHAR(255) NOT NULL,
    notification_timestamp  DATETIME NOT NULL
);

-- Table for admin decisions (ROLE: ADMIN)
CREATE TABLE IF NOT EXISTS admin_decisions (
    request_id  INT PRIMARY KEY AUTO_INCREMENT,
    user_id     INT,
    entry       TEXT NOT NULL,
    decision    ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
