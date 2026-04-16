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
    client_id       INT,
    description 	VARCHAR(255) NOT NULL,
    duration_hours 	INT NOT NULL,
    arrival_time 	DATETIME,
    deadline_time 	DATETIME,
    jobStatus 		ENUM ('in_progress', 'completed', 'rejected'),
    completionTime 	DATETIME
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- Table for the vehicles (ROLE: OWNER)
CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY,
    vehicle_name VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(255) NOT NULL,
    vehicle_status VARCHAR(255) NOT NULL,
    vehicle_availability BOOLEAN NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES jobs(job_id)
);

-- Table for the logs (ROLE: ADMIN)
CREATE TABLE logs (
    log_id INT PRIMARY KEY,
    log_message VARCHAR(255) NOT NULL,
    log_timestamp DATETIME NOT NULL,
    FOREIGN KEY (log_id) REFERENCES jobs(job_id)
);

-- Table for the notifications (ROLE: ADMIN)
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY,
    notification_message VARCHAR(255) NOT NULL,
    notification_timestamp DATETIME NOT NULL,
    FOREIGN KEY (notification_id) REFERENCES jobs(job_id)
);

-- Table for the admin decisions to request the decision from the admin (ROLE: ADMIN)
CREATE TABLE admin_decisions (
    request_id INT PRIMARY KEY,
    user_id INT,
    entry TEXT NOT NULL,
    decision ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
