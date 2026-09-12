# Smart Financial Expense Analysis

A modern, full-stack personal finance and expense analysis platform built with **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **MySQL**, and a responsive **Bootstrap 5** frontend.

---

## Table of Contents
1. [Project Name](#1-project-name)
2. [Project Purpose](#2-project-purpose)
3. [Current Project Status (Stage 5: Budget Management)](#3-current-project-status-stage-5-budget-management)
4. [Technology Stack](#4-technology-stack)
5. [Required Software](#5-required-software)
6. [Recommended Versions](#6-recommended-versions)
7. [Java 21 Installation Guide](#7-java-21-installation-guide)
8. [MySQL Installation Guide](#8-mysql-installation-guide)
9. [How to Create the Database](#9-how-to-create-the-database)
10. [How to Configure Environment Variables](#10-how-to-configure-environment-variables)
11. [How to Configure Database Credentials](#11-how-to-configure-database-credentials)
12. [How to Change the Server Port (Port Conflict Solution)](#12-how-to-change-the-server-port-port-conflict-solution)
13. [Windows Setup](#13-windows-setup)
14. [Linux Setup](#14-linux-setup)
15. [macOS Setup](#15-macos-setup)
16. [How to Run Using Maven Wrapper](#16-how-to-run-using-maven-wrapper)
17. [How to Build the Project](#17-how-to-build-the-project)
18. [How to Test the Application](#18-how-to-test-the-application)
19. [Health & API Endpoints Specification](#19-health-endpoint-specification)
   - [19.1 Authentication Endpoints Specification](#191-authentication-endpoints-specification)
   - [19.2 Expense Management Endpoints Specification](#192-expense-management-endpoints-specification)
   - [19.3 Budget Management Endpoints Specification](#193-budget-management-endpoints-specification)
20. [Common Errors and Solutions](#20-common-errors-and-solutions)
21. [Project Folder Structure](#21-project-folder-structure)
22. [Future Modules (Roadmap)](#22-future-modules-roadmap)
23. [Security Notes](#23-security-notes)
24. [Git & GitHub Setup Instructions](#24-git--github-setup-instructions)

---

## 1. Project Name
**Smart Financial Expense Analysis**  
*Artifact ID*: `smart-financial-expense-analysis`

---

## 2. Project Purpose
The goal of **Smart Financial Expense Analysis** is to give users full control and clarity over their financial life. It enables individuals to:
- Log and track day-to-day income and expenses across customizable categories.
- Define monthly and category budgets with real-time tracking.
- Uncover spending trends and anomalies using deterministic statistical analysis.
- View interactive dashboards, summaries, and financial reports.
- Maintain full data privacy by running on self-hosted or dedicated infrastructure without dependence on external AI APIs or third-party data tracking.

---

## 3. Current Project Status (Stage 5: Budget Management)
This project follows a structured 10-stage iterative development model:

* **Stage 1: Project Setup** (Completed)
  - Core Spring Boot 3 & Java 21 configuration.
  - Maven Wrapper integration for zero-install builds.
  - Portable database configuration reading from environment variables with safe defaults.
  - Portable HTTP server port configuration (`SERVER_PORT`).
  - Baseline health check endpoint (`/api/health`).
  - Responsive frontend interface to verify backend health and environment connectivity.

* **Stage 2: Database & Entities** (Completed)
  - Core JPA entities: `User`, `Category`, `Expense`, `Budget`.
  - Enums: `Role` (`USER`, `ADMIN`), `PaymentMethod` (`CASH`, `UPI`, `CARD`, `BANK_TRANSFER`, `OTHER`).
  - Spring Data JPA repositories with query methods: `UserRepository`, `CategoryRepository`, `ExpenseRepository`, `BudgetRepository`.
  - Database schema definition script (`database/schema.sql`).
  - Bean validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Positive`, `@Min`, `@Max`).
  - Integration test suite (`EntityRepositoryIntegrationTest`) validating CRUD operations, relationships, and uniqueness constraints.

* **Stage 3: Authentication & User Management** (Completed)
  - Spring Security session-based authentication (`JSESSIONID` cookie, stateful sessions, zero JWT complexity).
  - BCrypt password hashing (passwords never stored in plain text, never logged, never returned in responses).
  - User registration (`POST /api/auth/register`) with automatic `USER` role assignment.
  - User login (`POST /api/auth/login`) with credential validation and session establishment.
  - User logout (`POST /api/auth/logout`) with session invalidation.
  - Current user profile (`GET /api/auth/me`) returning safe user metadata without passwords.
  - Role-based authorization (`GET /api/admin/test` restricted strictly to `ADMIN` users).
  - Consistent JSON error response format (`ApiResponse`, `GlobalExceptionHandler`).
  - DTOs isolating JPA entities from REST API layer (`RegisterRequest`, `LoginRequest`, `UserResponse`, `AuthResponse`, `ApiResponse`).
  - Full integration test suite (`AuthControllerIntegrationTest`) with 10 passing tests.

* **Stage 4: Expense Management** (Completed)
  - Full CRUD operations for expenses (Add, Edit, Delete, View).
  - Category association and custom expense management.
  - Search, filtering, and date-range queries for expense records.
  - Ownership security ensuring users only access their own expenses.
  - Dynamic responsive UI (`expenses.html`, `js/expenses.js`).
  - Comprehensive integration test suite (`ExpenseControllerIntegrationTest`) with 27 passing tests.

* **Stage 5: Budget Management** (Active / Current)
  - Monthly budget planning and spending limit allocation.
  - Database-level spending calculation across calendar months.
  - Deterministic financial metrics: Remaining budget, utilization percentage, status rules.
  - Strict ownership security and duplicate prevention.
  - Responsive UI with capped visual progress bar and history table (`budgets.html`, `js/budgets.js`).
  - Full integration test suite (`BudgetControllerIntegrationTest`) with 26 passing tests.

* **Stages 6–10: Analytics, Dashboard, UI & Testing** (Upcoming)

---

### Core Entities & Database Design

#### 1. `User` Entity (`users` table)
- `id` (BIGINT, Primary Key, Auto-Increment)
- `name` (VARCHAR(100), Required)
- `email` (VARCHAR(150), Unique, Required, Valid Email format)
- `password` (VARCHAR(255), Required)
- `role` (`Role` Enum: `USER`, `ADMIN`, stored as VARCHAR)

#### 2. `Category` Entity (`categories` table)
- `id` (BIGINT, Primary Key, Auto-Increment)
- `name` (VARCHAR(100), Unique, Required)
- `description` (VARCHAR(255), Optional)

#### 3. `Expense` Entity (`expenses` table)
- `id` (BIGINT, Primary Key, Auto-Increment)
- `user` (Many-to-One with `User`, Foreign Key `user_id`, Required)
- `category` (Many-to-One with `Category`, Foreign Key `category_id`, Required)
- `amount` (DECIMAL(12, 2), Positive, Required)
- `date` (DATE, Required)
- `paymentMethod` (`PaymentMethod` Enum: `CASH`, `UPI`, `CARD`, `BANK_TRANSFER`, `OTHER`, stored as VARCHAR)
- `description` (VARCHAR(255), Optional)

#### 4. `Budget` Entity (`budgets` table)
- `id` (BIGINT, Primary Key, Auto-Increment)
- `user` (Many-to-One with `User`, Foreign Key `user_id`, Required)
- `month` (INT, 1 - 12, Required)
- `year` (INT, 2000+, Required)
- `amount` (DECIMAL(12, 2), Positive, Required)
- **Constraint**: Unique on `(user_id, month, year)` — ensures one budget allocation per user per calendar month.

---

## 4. Technology Stack
- **Language**: Java 21 (LTS)
- **Backend Framework**: Spring Boot 3.3.4
- **Web Layer**: Spring MVC (Embedded Apache Tomcat)
- **Persistence Layer**: Spring Data JPA / Hibernate
- **Database Driver**: MySQL Connector/J (`mysql-connector-j`)
- **Build System**: Maven 3.9+ with bundled Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla ES6+), Bootstrap 5.3, Bootstrap Icons
- **Database**: MySQL 8.x / MariaDB 10.x
- **Zero Third-Party AI / Zero Cloud API Keys**: Fully deterministic, local, and private.

---

## 5. Required Software
To run and develop this application, you only need:
1. **Java Development Kit (JDK) 21** (Required runtime and SDK).
2. **MySQL Server** (8.0+ or MariaDB 10.4+, standalone or via XAMPP/Docker).
3. **Git** (For version control).

> **Note**: You do **NOT** need Maven pre-installed globally because this repository includes the official **Maven Wrapper** (`mvnw` for Linux/macOS, `mvnw.cmd` for Windows).

---

## 6. Recommended Versions
| Tool | Recommended Version | Compatibility |
| :--- | :--- | :--- |
| **Java (JDK)** | **21 (LTS)** (Eclipse Temurin, Oracle, or OpenJDK) | Java 21+ |
| **Spring Boot** | **3.3.4** | Compatible with Spring Framework 6.1 |
| **MySQL** | **8.0+** or **MariaDB 10.4+** | Standard port 3306 |
| **Maven** | **3.9+** (Provided via `mvnw`) | Standard Maven 3 wrapper |
| **Operating System** | Windows 10/11, Ubuntu 20.04+, macOS 12+ | Fully platform-independent |

---

## 7. Java 21 Installation Guide

Verify if Java 21 is installed:
```bash
java -version
```
If you do not see Java 21:

### Windows:
1. Download Eclipse Temurin 21 (LTS) installer from [Adoptium](https://adoptium.net/) or Oracle JDK 21 from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21).
2. Run the `.msi` installer and ensure **"Set JAVA_HOME variable"** and **"Add to PATH"** are checked.
3. Restart your terminal and verify:
   ```powershell
   java -version
   ```

### Linux (Ubuntu / Debian):
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

### macOS:
Using Homebrew:
```bash
brew install openjdk@21
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
java -version
```

---

## 8. MySQL Installation Guide

### Windows (Option A - Standalone MySQL Server):
1. Download MySQL Community Server installer from [MySQL Developer Zone](https://dev.mysql.com/downloads/installer/).
2. Follow installer instructions, set the root user password, and enable the Windows service.

### Windows (Option B - XAMPP):
1. Download and install [XAMPP](https://www.apachefriends.org/).
2. Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.

### Linux (Ubuntu / Debian):
```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### macOS (Homebrew):
```bash
brew install mysql
brew services start mysql
```

---

## 9. How to Create the Database
The project includes a ready-to-run initialization script at `database/schema.sql`.

### Option A: Using MySQL CLI
```bash
mysql -u root -p < database/schema.sql
```
*(If your root user has no password, simply run: `mysql -u root < database/schema.sql`)*

### Option B: Using MySQL Workbench or phpMyAdmin (XAMPP)
1. Open MySQL Workbench or visit `http://localhost/phpmyadmin`.
2. Run the SQL query:
   ```sql
   CREATE DATABASE IF NOT EXISTS smart_financial_expense
       CHARACTER SET utf8mb4
       COLLATE utf8mb4_unicode_ci;
   ```

> **Note**: Spring Boot is also configured with `createDatabaseIfNotExist=true` in its connection string, so if your MySQL user has `CREATE` permissions, it can create the database automatically upon startup.

---

## 10. How to Configure Environment Variables
The application reads configuration from environment variables with sensible local development defaults:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP port on which Spring Boot listens | `8080` |
| `DB_HOST` | Hostname or IP address of MySQL | `localhost` |
| `DB_PORT` | Port MySQL is listening on | `3306` |
| `DB_NAME` | Database schema name | `smart_financial_expense` |
| `DB_USERNAME` | MySQL login username | `root` |
| `DB_PASSWORD` | MySQL login password | *(blank)* |

A template file named `.env.example` is included in the project root. You can refer to it when configuring your local environment.

---

## 11. How to Configure Database Credentials
If your local MySQL root user has a password (e.g., `MySecretPass123`):

### Windows (PowerShell):
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="MySecretPass123"
.\mvnw.cmd spring-boot:run
```

### Windows (Command Prompt):
```cmd
set DB_USERNAME=root
set DB_PASSWORD=MySecretPass123
mvnw.cmd spring-boot:run
```

### Linux / macOS (Bash / Zsh):
```bash
export DB_USERNAME="root"
export DB_PASSWORD="MySecretPass123"
./mvnw spring-boot:run
```

---

## 12. How to Change the Server Port (Port Conflict Solution)
If you encounter the error **"Port 8080 was already in use"**, you do **NOT** need to terminate any running processes. Simply pass a different port (such as `8081`, `8082`, or `9090`) using any of the methods below:

### Method 1: Environment Variable (Recommended)
- **Windows (PowerShell)**:
  ```powershell
  $env:SERVER_PORT="8081"
  .\mvnw.cmd spring-boot:run
  ```
- **Windows (CMD)**:
  ```cmd
  set SERVER_PORT=8081
  mvnw.cmd spring-boot:run
  ```
- **Linux / macOS**:
  ```bash
  SERVER_PORT=8081 ./mvnw spring-boot:run
  ```

### Method 2: Command-Line Argument
Works uniformly on all operating systems:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```
Or on Windows:
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Method 3: When Running the Packaged JAR
```bash
java -Dserver.port=8081 -jar target/smart-financial-expense-analysis-0.0.1-SNAPSHOT.jar
```

---

## 13. Windows Setup

1. **Clone the Repository**:
   ```cmd
   git clone <repository-url>
   cd "Smart Financial Expense Analysis"
   ```
2. **Verify Java 21**:
   ```cmd
   java -version
   ```
3. **Start MySQL** (Ensure service or XAMPP MySQL is running).
4. **Initialize Database** (Optional, recommended):
   ```cmd
   mysql -u root -p < database\schema.sql
   ```
5. **Run the Application**:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
6. **Open in Browser**:
   Visit [http://localhost:8080/](http://localhost:8080/) (or your chosen `SERVER_PORT`).

---

## 14. Linux Setup

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd "Smart Financial Expense Analysis"
   ```
2. **Grant Execute Permission to Maven Wrapper**:
   ```bash
   chmod +x mvnw
   ```
3. **Initialize Database**:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
4. **Run Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Open in Browser**:
   Visit `http://localhost:8080/`.

---

## 15. macOS Setup

1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd "Smart Financial Expense Analysis"
   ```
2. **Grant Execute Permission to Maven Wrapper**:
   ```bash
   chmod +x mvnw
   ```
3. **Initialize Database**:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
4. **Run Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Open in Browser**:
   Visit `http://localhost:8080/`.

---

## 16. How to Run Using Maven Wrapper
Maven Wrapper ensures every machine uses the exact same build environment without requiring global Maven installation:

- **Windows (Command Prompt / PowerShell)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

---

## 17. How to Build the Project

To compile, run tests, and package the application into an executable fat JAR:

- **Windows**:
  ```powershell
  .\mvnw.cmd clean package
  ```
- **Linux / macOS**:
  ```bash
  ./mvnw clean package
  ```

The built JAR file will be saved at:
`target/smart-financial-expense-analysis-0.0.1-SNAPSHOT.jar`

To run the built JAR directly:
```bash
java -jar target/smart-financial-expense-analysis-0.0.1-SNAPSHOT.jar
```

---

## 18. How to Test the Application

### 1. In Browser / Web UI
1. Navigate to `http://localhost:8080/` (or your configured port).
2. The UI automatically queries `/api/health` and shows a badge:
   - **Green ("Backend is ONLINE (UP)")**: Everything is running properly.
   - **Red ("Backend is unreachable")**: Server is stopped or port is blocked.

### 2. Using cURL
```bash
curl -i http://localhost:8080/api/health
```

### 3. Using PowerShell
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
```

---

## 19. Health Endpoint Specification

- **Method**: `GET`
- **Path**: `/api/health`
- **Response Format**: `application/json`
- **HTTP Status**: `200 OK`

### Example Response Body
```json
{
  "service": "Smart Financial Expense Analysis",
  "stage": "Stage 1 - Project Setup",
  "message": "Backend is running successfully!",
  "status": "UP",
  "timestamp": "2026-09-12T14:45:00.123456"
}
```

---

## 19.1 Authentication Endpoints Specification

### 1. Register a New User
- **Method**: `POST`
- **Path**: `/api/auth/register`
- **Payload**:
  ```json
  {
    "name": "Alex Smith",
    "email": "alex@example.com",
    "password": "strongPassword123"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "user": {
      "id": 1,
      "name": "Alex Smith",
      "email": "alex@example.com",
      "role": "USER"
    }
  }
  ```

### 2. Login
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Payload**:
  ```json
  {
    "email": "alex@example.com",
    "password": "strongPassword123"
  }
  ```
- **Response** (`200 OK` + Sets `JSESSIONID` Cookie):
  ```json
  {
    "success": true,
    "message": "Login successful",
    "user": {
      "id": 1,
      "name": "Alex Smith",
      "email": "alex@example.com",
      "role": "USER"
    }
  }
  ```

### 3. Current User Profile
- **Method**: `GET`
- **Path**: `/api/auth/me`
- **Header/Cookie**: Requires active session (`JSESSIONID`)
- **Response** (`200 OK` if authenticated, `401 Unauthorized` if not):
  ```json
  {
    "id": 1,
    "name": "Alex Smith",
    "email": "alex@example.com",
    "role": "USER"
  }
  ```

### 4. Logout
- **Method**: `POST`
- **Path**: `/api/auth/logout`
- **Response** (`200 OK` + Invalidates session):
  ```json
  {
    "success": true,
    "message": "Logged out successfully"
  }
  ```

### 5. Protected Admin Route
- **Method**: `GET`
- **Path**: `/api/admin/test`
- **Authorization**:
  - `ADMIN` role: `200 OK` (`{ "success": true, "message": "Welcome Admin! Role-based access verified." }`)
  - `USER` role: `403 Forbidden` (`{ "success": false, "message": "Access denied: You do not have permission to access this resource" }`)
  - Unauthenticated: `401 Unauthorized` (`{ "success": false, "message": "Authentication is required to access this resource" }`)

---

## 19.2 Expense Management Endpoints Specification

All expense endpoints require an active authenticated session (`JSESSIONID`). All operations strictly enforce that users can only view, create, modify, or delete their **own** expenses.

### 1. Create a New Expense
- **Method**: `POST`
- **Path**: `/api/expenses`
- **Header/Cookie**: Requires active session
- **Payload**:
  ```json
  {
    "amount": 500.00,
    "categoryId": 1,
    "date": "2026-09-12",
    "paymentMethod": "UPI",
    "description": "Dinner with friends"
  }
  ```
- **Validation Rules**:
  - `amount`: Required, `BigDecimal`, strictly greater than 0. Zero and negative values rejected.
  - `categoryId`: Required, must correspond to an existing Category. Nonexistent ID returns `404 Not Found`.
  - `date`: Required, valid `LocalDate` (`YYYY-MM-DD`).
  - `paymentMethod`: Required, must be one of: `CASH`, `UPI`, `CARD`, `BANK_TRANSFER`, `OTHER`.
  - `description`: Optional, maximum 255 characters, trimmed of whitespace.
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "amount": 500.00,
    "category": {
      "id": 1,
      "name": "Food",
      "description": "Groceries, dining out, restaurants, and food delivery"
    },
    "date": "2026-09-12",
    "paymentMethod": "UPI",
    "description": "Dinner with friends"
  }
  ```

### 2. View My Expenses (with Search & Filters)
- **Method**: `GET`
- **Path**: `/api/expenses`
- **Query Parameters** (All optional and combinable):
  - `search` (String): Case-insensitive text search matching the description (e.g. `?search=dinner`).
  - `categoryId` (Long): Filter by specific category ID (e.g. `?categoryId=1`).
  - `paymentMethod` (Enum): Filter by payment method (`CASH`, `UPI`, `CARD`, `BANK_TRANSFER`, `OTHER`).
  - `startDate` (ISO Date): Start date inclusive (e.g. `?startDate=2026-09-01`).
  - `endDate` (ISO Date): End date inclusive (e.g. `?endDate=2026-09-12`).
  - `page` (Integer): Zero-based page number (e.g. `?page=0`).
  - `size` (Integer): Page size (default/max sanitized, e.g. `?size=10`).
- **Sorting**: Default ordering is newest first (`date DESC, id DESC`).
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": 1,
      "amount": 500.00,
      "category": {
        "id": 1,
        "name": "Food",
        "description": "Groceries, dining out, restaurants, and food delivery"
      },
      "date": "2026-09-12",
      "paymentMethod": "UPI",
      "description": "Dinner with friends"
    }
  ]
  ```

### 3. View Single Expense by ID
- **Method**: `GET`
- **Path**: `/api/expenses/{id}`
- **Security**: If the expense does not exist OR belongs to another user, returns `404 Not Found` to prevent entity enumeration.
- **Response** (`200 OK` if owned, `404 Not Found` if missing or owned by another user):
  ```json
  {
    "id": 1,
    "amount": 500.00,
    "category": {
      "id": 1,
      "name": "Food",
      "description": "Groceries, dining out, restaurants, and food delivery"
    },
    "date": "2026-09-12",
    "paymentMethod": "UPI",
    "description": "Dinner with friends"
  }
  ```

### 4. Update an Expense
- **Method**: `PUT`
- **Path**: `/api/expenses/{id}`
- **Security**: Can only update expenses owned by the authenticated user. Attempting to update another user's expense returns `404 Not Found`.
- **Payload**:
  ```json
  {
    "amount": 650.00,
    "categoryId": 1,
    "date": "2026-09-12",
    "paymentMethod": "CARD",
    "description": "Updated dinner note"
  }
  ```
- **Response** (`200 OK` with updated expense, or `404 Not Found`):
  ```json
  {
    "id": 1,
    "amount": 650.00,
    "category": {
      "id": 1,
      "name": "Food",
      "description": "Groceries, dining out, restaurants, and food delivery"
    },
    "date": "2026-09-12",
    "paymentMethod": "CARD",
    "description": "Updated dinner note"
  }
  ```

### 5. Delete an Expense
- **Method**: `DELETE`
- **Path**: `/api/expenses/{id}`
- **Security**: Can only delete expenses owned by the authenticated user. Attempting to delete another user's expense returns `404 Not Found`.
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Expense deleted successfully"
  }
  ```

### 6. Get Available Categories
- **Method**: `GET`
- **Path**: `/api/categories`
- **Response** (`200 OK`):
  ```json
  [
    { "id": 1, "name": "Food", "description": "Groceries, dining out, restaurants, and food delivery" },
    { "id": 2, "name": "Travel", "description": "Flights, trains, fuel, taxis, and public transit" },
    { "id": 3, "name": "Shopping", "description": "Clothing, electronics, household goods, and personal items" },
    { "id": 4, "name": "Bills", "description": "Electricity, water, internet, phone, and utility bills" }
  ]
  ```

---

## 19.3 Budget Management Endpoints Specification

All budget endpoints require an authenticated Spring Security session (`JSESSIONID`). The system enforces strict user ownership: users can only view, create, update, or delete their own monthly budgets. Accessing or modifying another user's budget returns `404 Not Found` to prevent entity enumeration.

> [!NOTE]
> **Deterministic Rule-Based Calculations (No AI / No Machine Learning)**:
> All financial metrics (total expenses, remaining amount, utilization percentage, and status) are calculated using exact arithmetic (`BigDecimal` with `HALF_UP` rounding) and rule-based thresholds. There is **no external AI, no machine learning, and no external API key** involved.

### Financial Formulas
```
remainingAmount = budgetAmount - totalExpenses
utilizationPercentage = (totalExpenses / budgetAmount) × 100
```

### Status Rules
| Utilization Percentage | Status | Meaning |
| :--- | :--- | :--- |
| **< 80.00%** | `UNDER_BUDGET` | Spending is well within safe thresholds. |
| **80.00% – 100.00%** | `NEAR_LIMIT` | Approaching or exactly at the budget limit. |
| **> 100.00%** | `OVER_BUDGET` | Spending has exceeded the allocated budget. |

*Note*: If budget is ₹20,000 and expenses are ₹25,000, `remainingAmount` is `-₹5,000.00`, `utilizationPercentage` is `125.00%`, and status is `OVER_BUDGET`.

---

### 1. Create a Monthly Budget
- **Method**: `POST`
- **Path**: `/api/budgets`
- **Header/Cookie**: Requires active session
- **Payload**:
  ```json
  {
    "month": 9,
    "year": 2026,
    "amount": 20000.00
  }
  ```
- **Validation Rules**:
  - `month`: Required integer between `1` and `12`.
  - `year`: Required integer (`2000` or later).
  - `amount`: Required `BigDecimal`, strictly greater than 0 (`@Positive`).
  - `userId`: **Never accepted from frontend**; extracted directly from session.
  - **Uniqueness**: Only one budget per user per month/year. Attempting to create a duplicate returns `409 Conflict`.
- **Response** (`201 Created`):
  ```json
  {
    "id": 1,
    "month": 9,
    "year": 2026,
    "budgetAmount": 20000.00,
    "totalExpenses": 14500.00,
    "remainingAmount": 5500.00,
    "utilizationPercentage": 72.50,
    "status": "UNDER_BUDGET"
  }
  ```

### 2. Get Current Month Budget
- **Method**: `GET`
- **Path**: `/api/budgets/current`
- **Description**: Dynamically resolves the server's current month and year (e.g. September 2026) and returns the authenticated user's budget with live spending calculations.
- **Response** (`200 OK` if configured, `404 Not Found` if not configured):
  ```json
  {
    "id": 1,
    "month": 9,
    "year": 2026,
    "budgetAmount": 20000.00,
    "totalExpenses": 14500.00,
    "remainingAmount": 5500.00,
    "utilizationPercentage": 72.50,
    "status": "UNDER_BUDGET"
  }
  ```

### 3. Get Budget by Month and Year
- **Method**: `GET`
- **Path**: `/api/budgets?month=9&year=2026`
- **Parameters**: `month` (1–12), `year` (e.g. 2026)
- **Response** (`200 OK` if found, `404 Not Found` if not configured for requested month/year):
  ```json
  {
    "id": 1,
    "month": 9,
    "year": 2026,
    "budgetAmount": 20000.00,
    "totalExpenses": 14500.00,
    "remainingAmount": 5500.00,
    "utilizationPercentage": 72.50,
    "status": "UNDER_BUDGET"
  }
  ```

### 4. Get All My Budgets (Budget History)
- **Method**: `GET`
- **Path**: `/api/budgets/all`
- **Sorting**: Automatically sorted with newest first (`year DESC, month DESC`).
- **Ownership**: Returns strictly budgets owned by the authenticated user.
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": 1,
      "month": 9,
      "year": 2026,
      "budgetAmount": 20000.00,
      "totalExpenses": 14500.00,
      "remainingAmount": 5500.00,
      "utilizationPercentage": 72.50,
      "status": "UNDER_BUDGET"
    },
    {
      "id": 2,
      "month": 8,
      "year": 2026,
      "budgetAmount": 18000.00,
      "totalExpenses": 15300.00,
      "remainingAmount": 2700.00,
      "utilizationPercentage": 85.00,
      "status": "NEAR_LIMIT"
    }
  ]
  ```

### 5. Get Single Budget by ID
- **Method**: `GET`
- **Path**: `/api/budgets/{id}`
- **Security**: Verifies ownership. If the budget belongs to another user, returns `404 Not Found`.
- **Response** (`200 OK` if owned, `404 Not Found` if nonexistent or owned by another user).

### 6. Update Budget
- **Method**: `PUT`
- **Path**: `/api/budgets/{id}`
- **Payload**:
  ```json
  {
    "month": 9,
    "year": 2026,
    "amount": 25000.00
  }
  ```
- **Requirements**:
  - Verifies ownership (`404 Not Found` if unowned).
  - Validates month (1–12), year (>=2000), and amount (>0).
  - Prevents changing month/year to a combination that already exists (`409 Conflict`).
  - Preserves budget ID.
- **Response** (`200 OK` with updated `BudgetResponse`).

### 7. Delete Budget
- **Method**: `DELETE`
- **Path**: `/api/budgets/{id}`
- **Security**: Verifies ownership.
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Budget deleted successfully"
  }
  ```

---

## 20. Common Errors and Solutions

### 1. Error: `Port 8080 was already in use`
- **Cause**: Another service or application is occupying port 8080.
- **Solution**: Do not kill processes blindly. Configure `SERVER_PORT` to an available port (e.g., 8081):
  ```powershell
  $env:SERVER_PORT="8081"
  .\mvnw.cmd spring-boot:run
  ```

### 2. Error: `Communications link failure` / `Connection refused`
- **Cause**: MySQL server is not running or is on a different port.
- **Solution**:
  - Start MySQL service (or start MySQL in XAMPP).
  - Verify MySQL is listening on port 3306 (`netstat -an | grep 3306`).
  - If MySQL is on another port, set `DB_PORT`:
    ```powershell
    $env:DB_PORT="3307"
    ```

### 3. Error: `Access denied for user 'root'@'localhost'`
- **Cause**: The password for the MySQL `root` user does not match.
- **Solution**: Provide your MySQL password using the `DB_PASSWORD` environment variable:
  ```powershell
  $env:DB_PASSWORD="your_actual_password"
  .\mvnw.cmd spring-boot:run
  ```

### 4. Error: `Unsupported class file major version` / Java Version Mismatch
- **Cause**: Trying to compile or run with an older JDK (e.g., Java 8, 11, or 17).
- **Solution**: Install JDK 21 and configure your `JAVA_HOME` environment variable to point to JDK 21.

### 5. Error on Linux/macOS: `Permission denied: ./mvnw`
- **Cause**: The Maven Wrapper script lost its POSIX execute bit.
- **Solution**:
  ```bash
  chmod +x mvnw
  ```

---

## 21. Project Folder Structure

```
Smart Financial Expense Analysis/
├── .env.example                                  # Template for local environment variables
├── .gitignore                                    # Git exclusion rules (secrets, target, IDE files)
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar                     # Maven Wrapper binary
│       └── maven-wrapper.properties              # Maven distribution URL
├── mvnw                                          # Maven Wrapper script (Linux / macOS)
├── mvnw.cmd                                      # Maven Wrapper script (Windows)
├── pom.xml                                       # Maven Project Object Model (Dependencies, Java 21)
├── README.md                                     # Project documentation & guide
├── database/
│   └── schema.sql                                # Database schema & table creation script
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── smartfinancialexpenseanalysis/
    │   │           ├── SmartFinancialExpenseAnalysisApplication.java  # Main application entry point
    │   │           ├── config/
    │   │           │   ├── SecurityConfig.java                        # Spring Security configuration (BCrypt, sessions, CORS)
    │   │           │   ├── CategoryDataInitializer.java               # Seeds default categories idempotently
    │   │           │   ├── CustomAuthenticationEntryPoint.java        # 401 JSON handler for unauthenticated requests
    │   │           │   ├── CustomAccessDeniedHandler.java             # 403 JSON handler for forbidden requests
    │   │           │   └── CorsConfig.java                            # CORS configuration
    │   │           ├── controller/
    │   │           │   ├── AuthController.java                        # Auth endpoints (register, login, logout, me)
    │   │           │   ├── AdminTestController.java                   # Protected endpoint for ADMIN role verification
    │   │           │   ├── CategoryController.java                    # Category endpoints (/api/categories)
    │   │           │   ├── ExpenseController.java                     # Stage 4 Expense CRUD & Filter endpoints (/api/expenses)
    │   │           │   ├── BudgetController.java                      # Stage 5 Budget CRUD & Calculation endpoints (/api/budgets)
    │   │           │   └── HealthController.java                      # Health check REST controller (/api/health)
    │   │           ├── dto/                                           # Data Transfer Objects
    │   │           │   ├── RegisterRequest.java                       # Name, email, password
    │   │           │   ├── LoginRequest.java                          # Email, password
    │   │           │   ├── UserResponse.java                          # Safe user profile (id, name, email, role)
    │   │           │   ├── AuthResponse.java                          # Success, message, UserResponse
    │   │           │   ├── ApiResponse.java                           # Uniform JSON status response
    │   │           │   ├── CategoryResponse.java                      # Safe category details
    │   │           │   ├── ExpenseRequest.java                        # Amount, categoryId, date, paymentMethod, desc
    │   │           │   ├── ExpenseResponse.java                       # Safe expense details with nested CategoryResponse
    │   │           │   ├── BudgetRequest.java                         # Month, year, amount
    │   │           │   └── BudgetResponse.java                        # Budget amount, total expenses, remaining, utilization, status
    │   │           ├── entity/                                        # Core JPA Domain Entities & Enums
    │   │           │   ├── User.java                                  # User entity (role, unique email, BCrypt password)
    │   │           │   ├── Category.java                              # Category entity (unique name)
    │   │           │   ├── Expense.java                               # Expense entity (user, category, amount)
    │   │           │   ├── Budget.java                                # Budget entity (user, month, year, amount)
    │   │           │   ├── Role.java                                  # Role enum (USER, ADMIN)
    │   │           │   ├── PaymentMethod.java                         # PaymentMethod enum (CASH, UPI, etc.)
    │   │           │   └── BudgetStatus.java                          # BudgetStatus enum (UNDER_BUDGET, NEAR_LIMIT, OVER_BUDGET)
    │   │           ├── exception/                                     # Error handling
    │   │           │   ├── DuplicateResourceException.java            # 409 Conflict
    │   │           │   ├── UnauthorizedException.java                 # 401 Unauthorized
    │   │           │   ├── ResourceNotFoundException.java             # 404 Not Found
    │   │           │   ├── BadRequestException.java                   # 400 Bad Request
    │   │           │   └── GlobalExceptionHandler.java                # Uniform @RestControllerAdvice JSON handler
    │   │           ├── repository/                                    # Spring Data JPA Repositories
    │   │           │   ├── UserRepository.java                        # User queries (findByEmail, etc.)
    │   │           │   ├── CategoryRepository.java                    # Category queries (findByName, etc.)
    │   │           │   ├── ExpenseRepository.java                     # Expense queries, sum aggregation, specification executor
    │   │           │   ├── ExpenseSpecification.java                  # Dynamic specifications with ownership enforcement
    │   │           │   └── BudgetRepository.java                      # Budget queries (user, month, year, ordering)
    │   │           └── service/                                       # Business Logic Layer
    │   │               ├── AuthService.java                           # Registration, login, logout, current user
    │   │               ├── CustomUserDetailsService.java              # Spring Security UserDetails adapter
    │   │               ├── ExpenseService.java                        # Expense CRUD, search, filter, ownership logic
    │   │               └── BudgetService.java                         # Budget CRUD, metrics, status, ownership logic
    │   └── resources/
    │       ├── application.properties            # Portable Spring Boot configuration
    │       └── static/
    │           ├── index.html                    # Homepage & Stage status dashboard
    │           ├── expenses.html                 # Authenticated Expense Management interface
    │           ├── budgets.html                  # Authenticated Budget Management interface
    │           ├── css/
    │           │   └── style.css                 # Custom styles
    │           └── js/
    │               ├── app.js                    # Client logic for roadmap and health check
    │               ├── expenses.js               # Client logic for Expense CRUD & filtering
    │               └── budgets.js                # Client logic for Budget CRUD & progress bar
└── test/
    └── java/
        └── com/
            └── smartfinancialexpenseanalysis/
                ├── SmartFinancialExpenseAnalysisApplicationTests.java # Context load test
                ├── controller/
                │   ├── AuthControllerIntegrationTest.java             # Stage 3 Auth & Role test suite (10 tests)
                │   ├── ExpenseControllerIntegrationTest.java          # Stage 4 Expense test suite (27 tests)
                │   └── BudgetControllerIntegrationTest.java           # Stage 5 Budget test suite (26 tests)
                └── repository/
                    └── EntityRepositoryIntegrationTest.java           # Stage 2 Entity & Repo test suite (4 tests)
```

---

## 22. Future Modules (Roadmap)
- **Stage 1: Project Setup** *(Completed)*
- **Stage 2: Database & Entities** *(Completed)*
- **Stage 3: Authentication** *(Completed)*
- **Stage 4: Expense Management** *(Completed)*
- **Stage 5: Budget Management** *(Active / Current)*
- **Stage 6: Smart Analysis** *(Upcoming)*
- **Stage 7: Dashboard** *(Upcoming)*
- **Stage 8: Admin Panel** *(Upcoming)*
- **Stage 9: Validation & Security** *(Upcoming)*
- **Stage 10: Final UI Polish & Testing** *(Upcoming)*

---

## 23. Security Notes
- **Zero Committed Secrets**: Never commit real database passwords or credentials to Git.
- **Environment Isolation**: `.env` and `.env.*` files are explicitly ignored by `.gitignore`.
- **Sensible Defaults**: Development defaults (`root` user with empty password) are intended strictly for local development. Production deployments should always supply strong credentials via `DB_USERNAME` and `DB_PASSWORD`.
- **No Third-Party AI Data Sharing**: All expense computation and categorization logic runs on the local server without sending sensitive financial data to external AI APIs.

---

## 24. Git & GitHub Setup Instructions

1. **Check Repository Status**:
   ```bash
   git status
   ```
2. **Add Files to Staging**:
   ```bash
   git add .
   ```
3. **Commit Changes**:
   ```bash
   git commit -m "Configure portable Stage 1 setup with Java 21, environment variables, and schema"
   ```
4. **Push to Remote Repository**:
   ```bash
   git push origin main
   ```
