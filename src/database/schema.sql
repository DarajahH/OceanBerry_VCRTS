CREATE DATABASE IF NOT EXISTS vcrts_db;
USE vcrts_db;

CREATE TABLE IF NOT EXISTS users (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        ENUM('CLIENT', 'OWNER', 'ADMIN') NOT NULL DEFAULT 'CLIENT',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jobs (
    job_id          VARCHAR(50) PRIMARY KEY,
    submitter_id    VARCHAR(50),
    description     VARCHAR(255) NOT NULL,
    duration_hours  INT NOT NULL,
    arrival_time    DATETIME,
    deadline_time   DATETIME,
    jobStatus       ENUM('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED') DEFAULT 'QUEUED',
    completionTime  INT,
    vehicle_id      VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id            VARCHAR(50) PRIMARY KEY,
    owner_id              VARCHAR(50),
    vehicle_info          VARCHAR(255),
    vehicle_model         VARCHAR(100),
    vehicle_vin           VARCHAR(17),
    vehicle_make          VARCHAR(100),
    vehicle_year          VARCHAR(20),
    residency_hours       INT NOT NULL DEFAULT 0,
    vehicle_status        VARCHAR(50) DEFAULT 'IDLE',
    vehicle_availability  VARCHAR(20) DEFAULT 'open',
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP
);
