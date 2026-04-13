-- Create a database for the VCRTS project for Milestone 6
CREATE DATABASE IF NOT EXISTS vcrts_db;
USE vcrts_db

-- Table for the client jobs (ROLE: CLIENT)
CREATE TABLE jobs (
    job_id VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255),
    duration_hours INT,
    arrival_time DATETIME,
    deadline_time DATETIME,
    jobStatus ENUM ('in_progress', 'completed', 'rejected'),
    completionTime DATETIME
);
    
