-- ============================================================
-- HealthFirst Pharmacy - Pharmacy Inventory Management System
-- PRO632 | Database script (MySQL 8 / MariaDB 10)
-- Creates the database, all 5 tables and sample data.
-- ============================================================
DROP DATABASE IF EXISTS healthfirst_pims;
CREATE DATABASE healthfirst_pims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE healthfirst_pims;

-- Table 1: users (passwords are stored as SHA-256 hashes)
CREATE TABLE users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    role      ENUM('Admin','Cashier') NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- Table 2: suppliers
CREATE TABLE suppliers (
    supplier_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone          VARCHAR(20),
    email          VARCHAR(100),
    address        TEXT
);

-- Table 3: medicines
CREATE TABLE medicines (
    medicine_id       INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    company           VARCHAR(100),
    medicine_type     VARCHAR(50),
    price             DECIMAL(10,2) NOT NULL,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    reorder_level     INT NOT NULL DEFAULT 10,
    expiry_date       DATE NOT NULL,
    supplier_id       INT,
    CONSTRAINT fk_medicine_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(supplier_id) ON UPDATE CASCADE ON DELETE SET NULL
);

-- Table 4: sales (header)
CREATE TABLE sales (
    sale_id      INT AUTO_INCREMENT PRIMARY KEY,
    sale_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    user_id      INT NOT NULL,
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Table 5: sale_items (line items)
CREATE TABLE sale_items (
    sale_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    sale_id       INT NOT NULL,
    medicine_id   INT NOT NULL,
    quantity_sold INT NOT NULL,
    price_at_sale DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_item_sale     FOREIGN KEY (sale_id)     REFERENCES sales(sale_id) ON DELETE CASCADE,
    CONSTRAINT fk_item_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

-- ---------------------- SAMPLE DATA ----------------------
-- Logins: admin/admin123, cashier/cash123, thandi/cash123
INSERT INTO users (username, password, role, full_name) VALUES
('admin',   SHA2('admin123',256), 'Admin',   'System Administrator'),
('cashier', SHA2('cash123',256),  'Cashier', 'Sipho Dlamini'),
('thandi',  SHA2('cash123',256),  'Cashier', 'Thandi Mokoena');

INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
('MediSource Distributors',  'Anele Khumalo',  '0115550101', 'orders@medisource.example.co.za',  '12 Industrial Road, Johannesburg'),
('PharmaLink Wholesalers',   'Pieter van Wyk', '0125550102', 'sales@pharmalink.example.co.za',   '48 Church Street, Pretoria'),
('CarePlus Supplies',        'Naledi Sithole', '0215550103', 'info@careplus.example.co.za',      '5 Harbour Way, Cape Town'),
('Southern Cross Pharma',    'Ravi Naidoo',    '0315550104', 'trade@southerncross.example.co.za','77 Marine Parade, Durban'),
('Vital Health Traders',     'Lerato Molefe',  '0115550105', 'accounts@vitalhealth.example.co.za','230 Rivonia Road, Sandton');

INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
('Paracetamol 500mg Tablets',      'Zenith Labs',      'Tablet',    29.99, 150, 40, DATE_ADD(CURDATE(), INTERVAL 420 DAY), 1),
('Ibuprofen 400mg Tablets',        'Zenith Labs',      'Tablet',    45.50,  90, 30, DATE_ADD(CURDATE(), INTERVAL 380 DAY), 1),
('Amoxicillin 500mg Capsules',     'Novacare Pharma',  'Capsule',   89.00,  12, 25, DATE_ADD(CURDATE(), INTERVAL 25 DAY),  2),
('Cetirizine 10mg Tablets',        'Novacare Pharma',  'Tablet',    52.75,  60, 20, DATE_ADD(CURDATE(), INTERVAL 300 DAY), 2),
('Omeprazole 20mg Capsules',       'Bluepeak Health',  'Capsule',   98.40,  45, 20, DATE_ADD(CURDATE(), INTERVAL 500 DAY), 3),
('Metformin 500mg Tablets',        'Bluepeak Health',  'Tablet',    64.20,  80, 30, DATE_ADD(CURDATE(), INTERVAL 460 DAY), 3),
('Cough Syrup 100ml',              'Vitalis Remedies', 'Syrup',     74.99,  35, 15, DATE_ADD(CURDATE(), INTERVAL 18 DAY),  4),
('Vitamin C 1000mg Effervescent',  'Vitalis Remedies', 'Tablet',   110.00,   8, 15, DATE_ADD(CURDATE(), INTERVAL 240 DAY), 4),
('Oral Rehydration Salts Sachet',  'Aqua Health',      'Sachet',    18.50, 120, 40, DATE_ADD(CURDATE(), INTERVAL 330 DAY), 5),
('Hydrocortisone Cream 1% 30g',    'Dermacare',        'Cream',     85.90,  28, 10, DATE_ADD(CURDATE(), INTERVAL 270 DAY), 5),
('Diclofenac Injection 75mg',      'Medixa',           'Injection', 36.80,  40, 15, DATE_ADD(CURDATE(), INTERVAL 210 DAY), 1),
('Salbutamol Syrup 150ml',         'Novacare Pharma',  'Syrup',     69.95,  22, 10, DATE_SUB(CURDATE(), INTERVAL 12 DAY),  2),
('Loratadine 10mg Tablets',        'Zenith Labs',      'Tablet',    41.30,   5, 20, DATE_ADD(CURDATE(), INTERVAL 150 DAY), 1),
('Antiseptic Cream 50g',           'Dermacare',        'Cream',     49.99,  55, 15, DATE_ADD(CURDATE(), INTERVAL 600 DAY), 5),
('Ceftriaxone Injection 1g',       'Medixa',           'Injection',145.00,  18, 10, DATE_ADD(CURDATE(), INTERVAL 28 DAY),  3),
('Multivitamin Syrup 200ml',       'Vitalis Remedies', 'Syrup',     95.00,  30, 12, DATE_ADD(CURDATE(), INTERVAL 365 DAY), 4);

-- Sample sales history so the reports have data
INSERT INTO sales (sale_id, sale_date, total_amount, user_id) VALUES
(1, DATE_SUB(NOW(), INTERVAL 6 DAY), 112.73, 2),
(2, DATE_SUB(NOW(), INTERVAL 5 DAY), 120.49, 2),
(3, DATE_SUB(NOW(), INTERVAL 3 DAY), 153.90, 3),
(4, DATE_SUB(NOW(), INTERVAL 2 DAY), 335.89, 2),
(5, DATE_SUB(NOW(), INTERVAL 1 DAY), 169.70, 3),
(6, NOW(),                           178.97, 2);

INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES
(1, 1, 2,  29.99), (1, 4, 1,  52.75),
(2, 2, 1,  45.50), (2, 7, 1,  74.99),
(3, 5, 1,  98.40), (3, 9, 3,  18.50),
(4, 1, 1,  29.99), (4, 8, 2, 110.00), (4, 10, 1, 85.90),
(5, 6, 2,  64.20), (5, 13, 1, 41.30),
(6, 1, 3,  29.99), (6, 3, 1,  89.00);
