# 🫐 OceanBerry: VCRTS

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-GUI-4578E6?style=for-the-badge)
![JDBC](https://img.shields.io/badge/SQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Sockets](https://img.shields.io/badge/Multithreaded-Sockets-20232A?style=for-the-badge)

Welcome to the **OceanBerry Beta** of the **Vehicular Cloud Real-Time System (VCRTS)**. 

VCRTS is a distributed software platform designed to bridge the gap between clients needing computational resources and vehicle owners with idle computing power. The OceanBerry release introduces a completely revamped graphical user interface, a highly concurrent backend, and a transition to robust database management.

---

## ✨ What's New in OceanBerry (1.0)

* **Refreshed UI/UX:** A brand new, dark-mode-inspired Java Swing dashboard featuring our signature "OceanBerry" blue styling, dynamic hover states, and unified role-based navigation.
* **SQL Database Migration:** We have officially decoupled from flat-file storage. Data is now securely managed and queried via a dedicated `DatabaseService` using JDBC.
* **Multithreaded Architecture:** The backend `ServerMain` now utilizes a concurrent `ClientHandler` logic, allowing multiple clients, admins, and vehicle owners to interact with the system simultaneously without thread blocking.
* **Live Event Terminal:** A built-in terminal UI that monitors real-time server traffic, socket connections, and administrative decisions.

## 🚀 Core Features

### 👥 Role-Based Access Control
The OceanBerry dashboard dynamically adapts to the user's role:
* **Clients:** Submit computation jobs (duration, deadlines, and descriptions) and track their real-time approval status via active notification polling.
* **Vehicle Owners:** Register available vehicles, update residency times, and manage vehicle operational status (Usable, In Use, Maintenance).
* **Administrators:** A dedicated control center to review pending requests, accept/reject jobs, and calculate optimal First-In-First-Out (FIFO) completion times.

### ⚡ Real-Time Socket Communication
Client applications and the central `VCController` communicate via active Java Sockets, ensuring rapid data transmission and reliable network handshakes for all job requests.

---

## 🛠️ System Architecture

* **Frontend:** Java Swing (`JFrame`, `JTabbedPane`, `CardLayout`)
* **Backend:** Core Java, `java.net.Socket`, Multithreading (`Runnable`, `Thread`)
* **Database:** SQL via Java Database Connectivity (JDBC)
* **Design Pattern:** Model-View-Controller (MVC) separating UI dashboards from cloud data services and core job logic.

## 🏁 Getting Started

### Prerequisites
* Java Development Kit (JDK) 17 or higher
* A running SQL Database instance (ensure your JDBC drivers are correctly mapped in your classpath)

### Installation & Execution

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/yourusername/VCRTS-OceanBerry.git](https://github.com/yourusername/VCRTS-OceanBerry.git)

**Run Commands:**

Compile:
```bash
javac *.java
```

Run:
```bash
java Main
```

If using FlatLaf jar:
```bash
javac -cp ".:flatlaf-3.7.jar" *.java
java -cp ".:flatlaf-3.7.jar" Main
```
