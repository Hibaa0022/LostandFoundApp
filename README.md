# 🔍 Lost and Found Database Application

A lightweight Java-based management system designed to track and organize lost and found items. This project utilizes **JDBC (Java Database Connectivity)** to interface with a **MySQL** backend, providing a structured way to store, retrieve, and manage item records.

---

## 🛠️ Tech Stack
* **Language:** Java (JDK 8+)
* **Database:** MySQL
* **Driver:** MySQL Connector/J
* **Environment:** CLI (Command Line Interface) / IDE (IntelliJ/Eclipse)

---

## 📂 Project Structure

| File | Description |
| :--- | :--- |
| `LostFoundDB.sql` | The database script containing table schemas and initial data. |
| `LostFoundDBApp.java` | The main Java application logic and database connection bridge. |

---

## 🚀 Getting Started

Follow these steps to set up the project on your local machine.

### 1. Database Configuration
1. Open your **MySQL Workbench** or Terminal.
2. Create a new database:
   ```sql
   CREATE DATABASE LostFoundDB;

### Import the schema
* **In the terminal:** mysql -u root -p LostFoundDB < LostFoundDB.sql
* Or simply copy the contents of LostFoundDB.sql and run them in your SQL editor.


### 2. Java Setup
1. Download the MySQL Connector/J JAR file.
2. Open LostFoundDBApp.java and update the connection string with your MySQL credentials:
   ```Java
   private static final String URL = "jdbc:mysql://localhost:3306/LostFoundDB";
   private static final String USER = "your_username";
   private static final String PASS = "your_password";


### 3. Compilation & Execution
### Via Command Line:
 ```Bash
 # Compile
javac -cp ".;mysql-connector-java.jar" LostFoundDBApp.java

# Run
java -cp ".;mysql-connector-java.jar" LostFoundDBApp
 ```

* (Note: Use : instead of ; in the classpath if you are on macOS or Linux.)

### 📋 Features
* **Add New Records:** Log items with descriptions, dates, and locations.
* **Database Persistence:** Data is stored permanently in a relational database.
* **Status Management:** Easily differentiate between "Lost" and "Found" statuses.
* **Querying:** Search for specific items within the database.

### 📝 Usage Example
Upon running the application, you will be prompted to:
* **Report a Lost Item:** Input the name and description.
* **Report a Found Item:** Input the location where it was discovered.
* **View All Items:** Display a table of all current entries in the system.

### 📄 License
This project is for educational purposes and is open-source.
  





   
