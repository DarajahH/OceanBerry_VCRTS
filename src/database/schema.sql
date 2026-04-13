-- Create a database for the VCRTS project for Milestone 6
CREATE DATABASE IF NOT EXISTS vcrts_db;
USE vcrts_db

-- Users table for the user (help to replace the user.txt)
CREATE TABLE users (
	id 			INT PRIMARY KEY,
    username 	VARCHAR(50) NOT NULL UNIQUE,
    password 	VARCHAR(100) NOT NULL UNIQUE,
    role 		ENUM('CLIENT', 'OWNER', 'ADMIN') NOT NULL DEFAULT 'CLIENT',
    created_at 	DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Table for the client jobs (ROLE: CLIENT)
CREATE TABLE jobs (
    job_id 			INT PRIMARY KEY,
    description 	VARCHAR(255) NOT NULL,
    duration_hours 	INT NOT NULL,
    arrival_time 	DATETIME,
    deadline_time 	DATETIME,
    jobStatus 		ENUM ('in_progress', 'completed', 'rejected'),
    completionTime 	INT
);

CREATE TABLE vehicles_owner_submittor (
    id INT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    job_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (job_id) REFERENCES jobs(id)
);
