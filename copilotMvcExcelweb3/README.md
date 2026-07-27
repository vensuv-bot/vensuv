# copilotMvcExcelweb3

Spring Boot MVC application for managing token records with Excel (.xlsx) file storage.

## Quick Links

- **[QUICKSTART.md](QUICKSTART.md)** - Full documentation & API reference
- **Build**: `mvn clean package -DskipTests`
- **Run**: `mvn spring-boot:run` or `java -jar target/*.jar`
- **Access**: http://localhost:8080/dashboard

## Features

✓ Token Management Dashboard (Add/Edit/Delete)
✓ AJAX Token Detection/Verification
✓ Local Excel file storage (./data/tokens.xlsx)
✓ Bootstrap 5 UI with responsive table
✓ Dynamic row management in table
✓ Eclipse project structure for IDE import

## Project Structure

```
src/main/java/com/example/copilotmvc/
├── CopilotMvcExcelweb3Application.java  (Entry point)
├── model/TokenRecord.java               (Data model)
├── repository/ExcelTokenRepository.java (Excel CRUD)
└── controller/DashboardController.java  (REST/MVC endpoints)

src/main/resources/
├── templates/dashboard.html             (Thymeleaf UI)
└── application.properties               (Config)
```

## System Requirements

- Java 11+ (tested with Java 21)
- Maven 3.6+
- 50 MB disk space

For complete documentation, see **[QUICKSTART.md](QUICKSTART.md)**.
