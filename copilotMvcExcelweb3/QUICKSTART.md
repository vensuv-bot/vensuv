# CopilotMvcExcelweb3 - Spring Boot Token Dashboard

A professional Spring Boot 2.7 MVC application for managing token records with an Excel-based backend using Apache POI.

## ✨ Features

- **Token Management Dashboard**: View, add, and delete token records
- **Excel Storage**: Tokens stored in local Excel file (`./data/tokens.xlsx`)
- **CRUD Operations**:
  - ✓ Create tokens via form or inline table row
  - ✓ Read/list all tokens with live table updates
  - ✓ Delete tokens with confirmation
  - ✓ Detect/verify if token exists (AJAX support)
- **Responsive UI**: Bootstrap 5 styled dashboard
- **Dynamic Table Rows**: Add/edit/delete rows directly in the table
- **Eclipse Compatible**: Maven project with standard directory structure

## 📦 Project Structure

```
copilotMvcExcelweb3/
├── pom.xml                           # Maven dependencies & build config
├── src/
│   ├── main/
│   │   ├── java/com/example/copilotmvc/
│   │   │   ├── CopilotMvcExcelweb3Application.java  # Main entry point
│   │   │   ├── model/
│   │   │   │   └── TokenRecord.java  # Token data model (id, name, tokenValue)
│   │   │   ├── repository/
│   │   │   │   └── ExcelTokenRepository.java  # Excel CRUD operations (Apache POI)
│   │   │   └── controller/
│   │   │       └── DashboardController.java   # REST/MVC endpoints
│   │   ├── resources/
│   │   │   ├── templates/
│   │   │   │   └── dashboard.html    # Thymeleaf dashboard UI
│   │   │   └── application.properties # Spring configuration
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── web.xml           # Servlet config (IDE compatibility)
│   │           └── spring-mvc-servlet.xml  # Spring MVC config
│   └── target/
│       └── copilotMvcExcelweb3-0.0.1-SNAPSHOT.jar  # Built JAR

```

## 🚀 Quick Start

### Prerequisites
- **Java 11+** (or Java 21 LTS)
- **Maven 3.6+**

### Build

```bash
cd copilotMvcExcelweb3
mvn clean package -DskipTests
```

### Run

**Option 1: Spring Boot Maven Plugin**
```bash
mvn spring-boot:run
```

**Option 2: Execute JAR**
```bash
java -jar target/copilotMvcExcelweb3-0.0.1-SNAPSHOT.jar
```

**Option 3: Import into Eclipse**
1. File → Import → Maven → Existing Maven Projects
2. Select the `copilotMvcExcelweb3` folder
3. Eclipse will auto-detect the Maven project
4. Run as → Spring Boot App

### Access Dashboard

Open your browser and navigate to:
```
http://localhost:8080/dashboard
```

## 🎯 Dashboard Features

### Left Panel: Data Entry & Detection
- **Add / Register Token**: Form with Name and Token Value fields
- **Detect / Verify Token**: Check if a token exists (Form or AJAX)

### Right Panel: Tokens Table
- **Dynamic Table**: Shows all registered tokens with ID, Name, Token Value
- **+ Add Row**: Create inline editable rows for quick data entry
- **Delete**: Remove tokens with confirmation
- **Actions**: 
  - **+ Add New Token**: Jump to form section
  - **Refresh**: Reload page to see latest data

## 📊 Model: TokenRecord

```java
public class TokenRecord {
    private long id;           // Auto-increment ID
    private String name;       // Member/token name
    private String tokenValue; // Token string (API key, JWT, etc.)
}
```

## 🔧 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Display dashboard with all tokens |
| POST | `/dashboard/add` | Add new token (Form: name, tokenValue) |
| POST | `/dashboard/delete/{id}` | Delete token by ID |
| POST | `/dashboard/detect` | Verify token exists (Returns JSON or redirect) |

## 🗂️ Excel File Format

**Location**: `./data/tokens.xlsx` (auto-created)

| Column A | Column B | Column C |
|----------|----------|----------|
| id | name | tokenValue |
| 1 | API_Key_1 | sk_test_abc123... |
| 2 | JWT_Token | eyJhbGc... |

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Excel file path (relative or absolute)
excel.file.path=./data/tokens.xlsx

# Thymeleaf template caching
spring.thymeleaf.cache=false
```

## 📚 Dependencies

- **Spring Boot 2.7.18**: Web framework
- **Thymeleaf**: Template engine
- **Apache POI 5.2.3**: Excel file handling
- **SLF4J/Logback**: Logging

## 🛡️ Security Features

- HTML escaping in dynamic form submissions
- Confirmation dialogs for delete operations
- Server-side validation in repository methods
- Thread-safe Excel operations (synchronized lock)

## 🐛 Troubleshooting

### Excel file not found
- Ensure `./data/` directory exists or is writable
- Check `excel.file.path` in `application.properties`

### Port 8080 in use
- Change `server.port` in `application.properties`
- Or kill process: `lsof -i :8080` (Linux/Mac)

### Maven build fails
- Ensure Java 11+ is in PATH: `java -version`
- Clear Maven cache: `rm -rf ~/.m2/repository` (or Windows equivalent)
- Re-run: `mvn clean install`

## 📝 Example: Adding a Token via API

```bash
curl -X POST http://localhost:8080/dashboard/add \
  -d "name=MyAPI&tokenValue=secret_token_12345"
```

## 📋 Detecting Tokens

**Form Submit:**
```bash
curl -X POST http://localhost:8080/dashboard/detect \
  -d "tokenValue=secret_token_12345"
```

**AJAX (JSON Response):**
```bash
curl -X POST http://localhost:8080/dashboard/detect \
  -H "Accept: application/json" \
  -d "tokenValue=secret_token_12345"
```

Response:
```json
{ "exists": true }
```

## 🎓 For IDE Development

The project includes:
- ✓ `web.xml` for servlet container compatibility
- ✓ `spring-mvc-servlet.xml` for XML-based Spring config
- ✓ Maven standard directory layout for easy IDE import
- ✓ No external server required (embedded Tomcat)

## 📄 License

Open source - Free to use and modify

---

**Happy token management! 🎉**
