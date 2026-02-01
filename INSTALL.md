# Installation Guide for U-N-I-T-E

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
  ```bash
  java -version
  ```

- **Apache Maven 3.6 or higher**
  ```bash
  mvn -version
  ```

## Step-by-Step Installation

### 1. Clone or Navigate to the Project

```bash
cd U-N-I-T-E
```

### 2. Build the Project

```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile the source code
- Run tests
- Package the application

### 3. Run the Application

#### Option A: Using Maven
```bash
mvn spring-boot:run
```

#### Option B: Using the JAR file
```bash
java -jar target/unite-workflow-orchestrator-1.0.0-SNAPSHOT.jar
```

#### Option C: With a specific profile
```bash
# Development (default)
mvn spring-boot:run

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Verify Installation

Once the application starts, you should see output similar to:
```
Started UniteApplication in X.XXX seconds
```

Visit the following URLs to verify:

- **Health Check**: http://localhost:8080/actuator/health
- **H2 Console**: http://localhost:8080/h2-console
- **WebSocket Endpoint**: ws://localhost:8080/ws

## Database Setup

### Development (H2 In-Memory Database)

No setup required! H2 database runs in-memory by default.

Access the H2 console at: http://localhost:8080/h2-console

Connection details:
- JDBC URL: `jdbc:h2:mem:unite_db`
- Username: `sa`
- Password: (leave empty)

### Production (PostgreSQL)

1. Install PostgreSQL:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql

   # macOS
   brew install postgresql
   ```

2. Create database and user:
   ```sql
   CREATE DATABASE unite;
   CREATE USER unite WITH PASSWORD 'your-password';
   GRANT ALL PRIVILEGES ON DATABASE unite TO unite;
   ```

3. Update configuration:

   Create `application-prod.yml` or set environment variables:
   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/unite
   export DATABASE_USERNAME=unite
   export DATABASE_PASSWORD=your-password
   ```

4. Run with production profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

## Integration with workflow-springboot-starter

The application is designed to work with the `workflow-springboot-starter` library. To fully integrate:

### Option 1: Use Local Build

1. Clone and build the workflow-springboot-starter:
   ```bash
   git clone https://github.com/A-N-O-D-E-R/workflow-springboot-starter.git
   cd workflow-springboot-starter
   mvn clean install
   ```

2. Uncomment the dependency in `pom.xml`:
   ```xml
   <dependency>
     <groupId>com.anode</groupId>
     <artifactId>workflow-spring-boot-starter</artifactId>
     <version>0.0.1</version>
   </dependency>
   ```

3. Uncomment workflow configuration in `application.yml`:
   ```yaml
   workflow:
     enabled: true
     storage:
       type: jpa
   ```

4. Update `WorkflowExecutionService.java` to use actual RuntimeService

### Option 2: Use as Standalone

The current implementation includes a simulated workflow execution system that demonstrates the API structure without requiring the actual workflow engine.

## Testing the Installation

### 1. Create a Workflow Definition

```bash
curl -X POST http://localhost:8080/api/v1/workflow-definitions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-workflow",
    "description": "Test workflow",
    "version": "1.0.0",
    "definitionJson": {
      "steps": [
        {"id": "step1", "type": "test"}
      ]
    },
    "active": true
  }'
```

### 2. Execute the Workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflow-executions \
  -H "Content-Type: application/json" \
  -d '{
    "workflowDefinitionId": "<WORKFLOW_ID_FROM_PREVIOUS_STEP>",
    "inputVariables": {
      "test": "value"
    }
  }'
```

### 3. Test WebSocket Connection

Use a WebSocket client or the provided HTML example:

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <script>
        const socket = new SockJS('http://localhost:8080/ws');
        const stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);

            stompClient.subscribe('/topic/workflow-events', function(message) {
                console.log('Event received:', JSON.parse(message.body));
            });
        });
    </script>
</body>
</html>
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `application.yml`:
```yaml
server:
  port: 8081
```

### Database Connection Issues

For PostgreSQL issues:
1. Verify PostgreSQL is running:
   ```bash
   sudo service postgresql status
   ```

2. Check connection details in application-prod.yml

3. Verify database exists:
   ```bash
   psql -U unite -d unite
   ```

### Maven Build Failures

Clear Maven cache and rebuild:
```bash
mvn clean
rm -rf ~/.m2/repository
mvn install
```

### Lombok Issues in IDE

If your IDE doesn't recognize Lombok annotations:

1. Install Lombok plugin for your IDE
2. Enable annotation processing in IDE settings

## Next Steps

- Read the [README.md](README.md) for API documentation
- Explore the codebase structure
- Customize workflow definitions for your use case
- Integrate with the actual workflow engine

## Support

For issues and questions:
- Check the [workflow-springboot-starter documentation](https://github.com/A-N-O-D-E-R/workflow-springboot-starter)
- Review [workflow concepts](https://github.com/A-N-O-D-E-R/workflow)
