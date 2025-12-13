-- ================================
-- LOST & FOUND MANAGEMENT SYSTEM
-- FULL DATABASE SCRIPT
-- Run this in phpMyAdmin or MySQL Workbench
-- ================================

CREATE DATABASE IF NOT EXISTS lostfound_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lostfound_db;

-- USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin','staff') DEFAULT 'staff',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ITEMS TABLE
CREATE TABLE IF NOT EXISTS items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(80),
    location_found VARCHAR(150),
    date_found DATE,
    status ENUM('Found','Pending','Returned') DEFAULT 'Found',
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_user FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- CLAIMS TABLE
CREATE TABLE IF NOT EXISTS claims (
    claim_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    claimer_name VARCHAR(100),
    claimer_contact VARCHAR(80),
    claim_reason TEXT,
    claim_status ENUM('Pending','Approved','Rejected') DEFAULT 'Pending',
    claim_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_item FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);

-- ITEM LOGS TABLE (status change history)
CREATE TABLE IF NOT EXISTS item_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- STORED PROCEDURES
-- ================================
DELIMITER $$
CREATE PROCEDURE add_item (
    IN p_item_name VARCHAR(150),
    IN p_description TEXT,
    IN p_category VARCHAR(80),
    IN p_location VARCHAR(150),
    IN p_date DATE,
    IN p_created_by INT
)
BEGIN
    INSERT INTO items (item_name, description, category, location_found, date_found, created_by)
    VALUES (p_item_name, p_description, p_category, p_location, p_date, p_created_by);
END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE approve_claim (
    IN p_claim_id INT
)
BEGIN
    -- set claim approved
    UPDATE claims SET claim_status = 'Approved' WHERE claim_id = p_claim_id;

    -- set item status to Returned
    UPDATE items
    SET status = 'Returned'
    WHERE item_id = (SELECT item_id FROM claims WHERE claim_id = p_claim_id LIMIT 1);
END $$
DELIMITER ;

-- ================================
-- TRIGGERS
-- ================================
DELIMITER $$
CREATE TRIGGER trg_claim_after_insert
AFTER INSERT ON claims
FOR EACH ROW
BEGIN
    -- set item status to Pending when claim created
    UPDATE items SET status = 'Pending' WHERE item_id = NEW.item_id;
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_items_before_update
BEFORE UPDATE ON items
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO item_logs(item_id, old_status, new_status)
        VALUES (OLD.item_id, OLD.status, NEW.status);
    END IF;
END $$
DELIMITER ;

-- ================================
-- SAMPLE DATA
-- ================================
INSERT INTO users (full_name, email, password, role) VALUES
('Admin User','admin@gmail.com','admin123','admin'),
('Staff Member','staff@gmail.com','staff123','staff');

INSERT INTO items (item_name, description, category, location_found, date_found, created_by) VALUES
('Brown Wallet','Brown leather wallet with student ID','Personal','Main Cafeteria','2025-11-20',2),
('White Wallet','White leather wallet with student ID','Personal','Main Cafeteria','2025-12-20',2),
('Silver Bracelet','Thin silver bracelet with small stone','Jewelry','Library','2025-11-18',2);

-- End of SQL script
