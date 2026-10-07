# springboot-mysql-thymeleaf-devsecops

A small **Employee Management** web app built with Spring Boot, Thymeleaf and MySQL. It is designed as a simple learning project for a DevSecOps CI/CD pipeline (GitHub Actions → SonarQube → OWASP Dependency-Check → Gitleaks → Docker → Trivy → AWS ECR → ECS Fargate → OWASP ZAP).

## 1. Project overview

* A Thymeleaf UI to list, add, edit and delete employees.
* A JSON REST API for the same operations (handy for testing and OWASP ZAP).
* `/actuator/health` for ECS / pipeline health checks.
* No secrets in the code: database settings come from environment variables.

## 2. Architecture

```text
Browser ──► Thymeleaf pages ┐
                            ├─► Controller ─► Service ─► Repository ─► MySQL
REST client ─► /api/v1/... ─┘
```

## 3. Technology stack

Java 21 · Spring Boot 3.5 · Maven · Spring Web · Spring Data JPA · Thymeleaf · MySQL 8 · Lombok · Bean Validation · Spring Boot Actuator · Bootstrap 5 (CDN) · JUnit 5 · Mockito · Docker

## 4. Folder structure

```text
.
├── Dockerfile                  multi-stage build (Maven -> JRE 21 runtime)
├── docker-compose.yml          app + MySQL 8
├── .env.example                template for local credentials (copy to .env)
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example/devsecops
    │   │   ├── DevsecopsApplication.java
    │   │   ├── controller      EmployeeWebController (UI), EmployeeController (REST)
    │   │   ├── service         EmployeeService
    │   │   ├── repository      EmployeeRepository
    │   │   ├── entity          Employee
    │   │   ├── dto             EmployeeRequest, EmployeeResponse, ErrorResponse
    │   │   ├── exception       custom exceptions + GlobalExceptionHandler
    │   │   └── config          AppConfig (placeholder)
    │   └── resources
    │       ├── application.properties
    │       ├── templates       employees.html, employee-form.html
    │       └── static/css      style.css
    └── test/java/...           EmployeeServiceTest, EmployeeControllerTest
```

## 5. MySQL setup (running without Docker)

Install MySQL 8, then create the database (the app can also create it automatically):

```sql
CREATE DATABASE IF NOT EXISTS devsecops_db;
```

Tables are created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`, for local development only).

## 6. Environment variables

| Variable      | Default (local only)                                                    | Purpose           |
|---------------|-------------------------------------------------------------------------|-------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/devsecops_db?createDatabaseIfNotExist=true` | JDBC URL          |
| `DB_USERNAME` | `root`                                                                  | DB user           |
| `DB_PASSWORD` | `root`                                                                  | DB password       |

The defaults are for a local dev database only. In Docker/ECS always pass real values through environment variables or a secrets store (for ECS: AWS Secrets Manager / SSM). Never commit a `.env` file.

PowerShell example:

```powershell
$env:DB_PASSWORD = "your-local-password"
```

Bash example:

```bash
export DB_PASSWORD='your-local-password'
```

## 7. Run Spring Boot

```bash
./mvnw clean test            # run the tests
./mvnw clean package         # build target/*.jar
./mvnw spring-boot:run       # start the app on http://localhost:8080
```

(On Windows use `mvnw.cmd` in cmd/PowerShell.)

## 8. Thymeleaf UI

Open <http://localhost:8080>.

| URL                      | Page                                    |
|--------------------------|-----------------------------------------|
| `/`                      | redirects to `/employees`               |
| `/employees`             | employee list                           |
| `/employees/new`         | add employee form                       |
| `/employees/edit/{id}`   | edit employee form                      |
| `/employees/delete/{id}` | delete (asks for browser confirmation)  |

Validation messages appear next to the fields; success/error messages appear as Bootstrap alerts.

## 9. REST APIs

| Method | URL                      | Success        | Errors                           |
|--------|--------------------------|----------------|----------------------------------|
| POST   | `/api/v1/employees`      | 201 Created    | 400 validation, 409 duplicate email |
| GET    | `/api/v1/employees`      | 200 OK         |                                  |
| GET    | `/api/v1/employees/{id}` | 200 OK         | 404 not found                    |
| PUT    | `/api/v1/employees/{id}` | 200 OK         | 400, 404, 409                    |
| DELETE | `/api/v1/employees/{id}` | 204 No Content | 404                              |

Sample body:

```json
{ "name": "Alice", "email": "alice@example.com", "department": "IT", "salary": 5000 }
```

Error response:

```json
{ "timestamp": "2026-01-01T10:00:00Z", "status": 404, "message": "Employee not found with id 99" }
```

curl examples:

```bash
curl -i -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","department":"IT","salary":5000}'

curl http://localhost:8080/api/v1/employees
curl http://localhost:8080/api/v1/employees/1

curl -i -X PUT http://localhost:8080/api/v1/employees/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice B","email":"alice@example.com","department":"HR","salary":6000}'

curl -i -X DELETE http://localhost:8080/api/v1/employees/1
```

## 10. Docker build

```bash
docker build -t springboot-thymeleaf-app .
```

Run it against a MySQL you already have:

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/devsecops_db \
  -e DB_USERNAME=root \
  -e DB_PASSWORD="$DB_PASSWORD" \
  springboot-thymeleaf-app
```

The image runs as a non-root user and contains no credentials.

## 11. Docker Compose

Compose needs `DB_PASSWORD`. Either export it or copy `.env.example` to `.env` (git-ignored) and edit it.

```bash
export DB_PASSWORD='your-local-password'
docker compose up --build
```

* App: <http://localhost:8080>
* MySQL: `localhost:3306` (if 3306 is already used by a local MySQL, run with `MYSQL_HOST_PORT=3307`)

Stop it with `docker compose down` (add `-v` to also delete the database volume).

## 12. Testing

```bash
./mvnw clean test
```

* `EmployeeServiceTest` – JUnit 5 + Mockito, no database needed.
* `EmployeeControllerTest` – Spring `@WebMvcTest` + MockMvc for both the REST API and the Thymeleaf pages.

## 13. Actuator health check

```bash
curl http://localhost:8080/actuator/health
```

```json
{ "status": "UP" }
```

Only the `health` endpoint is exposed. Use it for the ECS task-definition health check and the pipeline's post-deploy check.

## Security notes

* Never commit AWS keys, DB passwords, API keys or `.env` files (see `.gitignore`).
* `/employees/delete/{id}` uses GET because the exercise specifies that URL; there is no authentication or CSRF protection yet. A real app should use a POST/DELETE with CSRF protection and add authentication. OWASP ZAP will likely flag this, which is useful for learning.
