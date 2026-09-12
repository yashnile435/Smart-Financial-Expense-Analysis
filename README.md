# Smart Financial Expense Analysis

A modern, full-stack personal finance and expense analysis platform built with **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **MySQL**, and a responsive **Bootstrap 5** frontend.

---

## Table of Contents
1. [Project Name](#1-project-name)
2. [Project Purpose](#2-project-purpose)
3. [Current Project Status (Stage 1)](#3-current-project-status-stage-1)
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
19. [Health Endpoint Specification](#19-health-endpoint-specification)
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

## 3. Current Project Status (Stage 1)
This project follows a structured 10-stage iterative development model:

* **Current Stage: Stage 1 — Project Setup** (Active)
  - Core Spring Boot 3 & Java 21 configuration.
  - Maven Wrapper integration for zero-install builds.
  - Portable database configuration reading from environment variables with safe defaults.
  - Portable HTTP server port configuration (`SERVER_PORT`).
  - Baseline health check endpoint (`/api/health`).
  - Responsive frontend interface to verify backend health and environment connectivity.
  - Machine-agnostic repository structure without local paths or committed secrets.

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
│   └── schema.sql                                # Stage 1 database creation script
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── smartfinancialexpenseanalysis/
    │   │           ├── SmartFinancialExpenseAnalysisApplication.java  # Main application entry point
    │   │           ├── config/
    │   │           │   ├── CorsConfig.java                            # CORS configuration
    │   │           │   └── package-info.java
    │   │           ├── controller/
    │   │           │   ├── HealthController.java                      # Health check REST controller
    │   │           │   └── package-info.java
    │   │           ├── entity/                                        # (Prepared for Stage 2)
    │   │           │   └── package-info.java
    │   │           ├── repository/                                    # (Prepared for Stage 2)
    │   │           │   └── package-info.java
    │   │           └── service/                                       # (Prepared for Stage 2)
    │   │               └── package-info.java
    │   └── resources/
    │       ├── application.properties            # Portable Spring Boot configuration
    │       └── static/
    │           ├── index.html                    # Frontend verification dashboard
    │           ├── css/
    │           │   └── style.css                 # Custom styles
    │           └── js/
    │               └── app.js                    # Client logic for health verification
    └── test/
        └── java/
            └── com/
                └── smartfinancialexpenseanalysis/
                    └── SmartFinancialExpenseAnalysisApplicationTests.java # Context load test
```

---

## 22. Future Modules (Roadmap)
- **Stage 1: Project Setup** *(Completed)*
- **Stage 2: Database & Entities** (Users, Expenses, Categories, Budgets)
- **Stage 3: Authentication** (User Registration, Login, Session/JWT Security, Roles)
- **Stage 4: Expense Management** (Full CRUD, Categorization, Date Search, Filters)
- **Stage 5: Budget Management** (Monthly Budgets, Category Limits, Overspend Warnings)
- **Stage 6: Smart Analysis** (Monthly/Weekly Summaries, Spending Pattern Detection)
- **Stage 7: Dashboard** (Visual Graphs, Metrics, Analytics Cards, Transaction Tables)
- **Stage 8: Admin Panel** (User Management, System Categories, Global Stats)
- **Stage 9: Validation & Security** (Input Sanitization, Custom Exceptions, Audit Logs)
- **Stage 10: Final UI Polish & Testing** (Responsive Design, End-to-End Testing)

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
