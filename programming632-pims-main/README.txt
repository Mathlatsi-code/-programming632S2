=====================================================================
 HEALTHFIRST PHARMACY - PHARMACY INVENTORY MANAGEMENT SYSTEM (PIMS)
 PRO632 Assignment | Java Swing/AWT + JDBC + MySQL
 GitHub repository: <PASTE YOUR PUBLIC REPO LINK HERE>
=====================================================================

DEFAULT LOGIN CREDENTIALS
---------------------------------------------------------------------
 Administrator :  admin    /  admin123
 Cashier       :  cashier  /  cash123
 Cashier (2nd) :  thandi   /  cash123

HOW TO RUN
---------------------------------------------------------------------
 1. Start MySQL (XAMPP: click "Start" next to MySQL). Default settings
    expected: localhost:3306, user "root", empty password.
    (If yours differs, edit db.properties.)
 2. Run the application using ONE of these:
      - yourname_pims.exe   (the Windows executable)
      - run.bat             (or:  java -jar pims.jar)
 3. On the very first run the application creates the database
    "healthfirst_pims" automatically from database.sql, including the
    tables and sample data (3 users, 5 suppliers, 16 medicines, 6 sales).
    You can also import database.sql manually in MySQL Workbench or
    phpMyAdmin.

REQUIREMENTS
---------------------------------------------------------------------
 - Java 11 or newer (JRE/JDK)   - MySQL 8 or XAMPP (MariaDB)
 - JDBC driver: lib/mariadb-java-client.jar is included. If you use
   MySQL 8 with its default authentication, place mysql-connector-j
   (renamed lib/mysql-connector-j.jar) in lib/ - it is picked up
   automatically.

FEATURES BY ROLE
---------------------------------------------------------------------
 LOGIN        Secure login (passwords stored as SHA-256 hashes), error
              messages for failed attempts, lock-out after 5 failures,
              role-based redirection to Admin or Cashier dashboard.

 CASHIER      Point of Sale: search medicines, add to cart, remove item,
              clear cart, checkout. Stock is checked and reduced inside
              a database transaction; expired or out-of-stock medicines
              cannot be sold. A Bill window is shown after checkout
              (Print / Save to file - saved in the "bills" folder).
              Stock Check: view price and availability only (read-only).

 ADMIN        Overview: headline numbers.
              Manage Medicines: full CRUD (expired = red, low stock = amber).
              Manage Suppliers: full CRUD.
              Manage Users: create / delete cashiers, reset passwords.
              Reports (with CSV export):
                1. Sales Report         (date range)
                2. Item-Wise Sales      (date range)
                3. Low Stock Report     (stock <= reorder level)
                4. Expiry Report        (expired or expiring within 1 month)

DATABASE
---------------------------------------------------------------------
 Tables: users, suppliers, medicines, sales, sale_items (see database.sql)
 PK/FK relationships: medicines->suppliers, sales->users,
 sale_items->sales and sale_items->medicines.
 Medicines that appear in past sales and cashiers who processed sales
 cannot be deleted (protects the sales history); the app explains this.

PROJECT STRUCTURE
---------------------------------------------------------------------
 /src/pims          All .java source files
 /screenshots       Required screenshots
 database.sql       CREATE DATABASE, CREATE TABLE and sample INSERTs
 build.bat          Compiles and builds pims.jar
 build_exe.bat      Builds the Windows .exe (jpackage)
 README.txt         This file
