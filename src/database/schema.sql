-- Create a database for the VCRTS project for Milestone 6
CREATE DATABASE IF NOT EXISTS vcrts_db;
USE vcrts_db

-- Table for the client jobs (ROLE: CLIENT)
CREATE TABLE jobs (
	id INT PRIMARY KEY,
    job_id VARCHAR(50),
    description VARCHAR(255),
    duration_hours  INT,
    arrival_time    DATETIME
);
    