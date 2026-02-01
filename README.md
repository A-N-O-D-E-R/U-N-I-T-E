# U-N-I-T-E - Workflow Orchestrator API

U-N-I-T-E is a comprehensive workflow orchestrator API built with Spring Boot, designed to manage and execute workflows based on the [workflow-springboot-starter](https://github.com/A-N-O-D-E-R/workflow-springboot-starter) and [workflow](https://github.com/A-N-O-D-E-R/workflow) concepts.

## Features

- **CRUD API for Workflow Definitions**: Create, read, update, and delete workflow definitions with JSON-based configurations
- **Workflow Execution Engine**: Execute workflows with support for parallel execution
- **Real-time Event Streaming**: WebSocket support for subscribing to workflow events
- **Async Processing**: Run multiple workflows in parallel using async execution
- **Batch Execution**: Execute multiple workflows simultaneously
- **Status Tracking**: Monitor workflow execution status and history
- **RESTful API**: Clean and well-documented REST endpoints

## Technology Stack

- **Spring Boot 3.2.0**
- **Java 17**
- **Spring Data JPA**
- **Spring WebSocket (STOMP)**
- **H2 Database** (development)
- **PostgreSQL** (production)
- **Lombok**
- **Jackson** for JSON processing

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Building the Project

```bash
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### H2 Console

Access the H2 database console at `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:unite_db`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

### Workflow Definition Management

#### Create Workflow Definition
```http
POST /api/v1/workflow-definitions
Content-Type: application/json

{
  "name": "order-processing",
  "description": "Order processing workflow",
  "version": "1.0.0",
  "definitionJson": {
    "steps": [
      {"id": "validate", "type": "validation"},
      {"id": "process", "type": "processing"}
    ]
  },
  "active": true,
  "createdBy": "admin",
  "tags": "order,payment"
}
```

#### Get Workflow Definition
```http
GET /api/v1/workflow-definitions/{id}
```

#### List All Workflow Definitions
```http
GET /api/v1/workflow-definitions
GET /api/v1/workflow-definitions?active=true
GET /api/v1/workflow-definitions?search=order
```

#### Update Workflow Definition
```http
PUT /api/v1/workflow-definitions/{id}
Content-Type: application/json

{
  "name": "order-processing",
  "version": "1.0.1",
  ...
}
```

#### Delete Workflow Definition
```http
DELETE /api/v1/workflow-definitions/{id}
```

#### Activate/Deactivate Workflow
```http
POST /api/v1/workflow-definitions/{id}/activate
POST /api/v1/workflow-definitions/{id}/deactivate
```

### Workflow Execution

#### Execute Workflow (Synchronous)
```http
POST /api/v1/workflow-executions
Content-Type: application/json

{
  "workflowDefinitionId": "workflow-def-id",
  "caseId": "case-123",
  "inputVariables": {
    "orderId": "12345",
    "amount": 100.00
  }
}
```

#### Execute Workflow (Asynchronous)
```http
POST /api/v1/workflow-executions?async=true
Content-Type: application/json

{
  "workflowDefinitionId": "workflow-def-id",
  "inputVariables": {
    "orderId": "12345"
  }
}
```

#### Batch Execute Workflows (Parallel)
```http
POST /api/v1/workflow-executions/batch
Content-Type: application/json

[
  {
    "workflowDefinitionId": "workflow-def-id-1",
    "inputVariables": {"orderId": "12345"}
  },
  {
    "workflowDefinitionId": "workflow-def-id-2",
    "inputVariables": {"orderId": "67890"}
  }
]
```

#### Get Execution Status
```http
GET /api/v1/workflow-executions/{executionId}
```

#### List Executions
```http
GET /api/v1/workflow-executions
GET /api/v1/workflow-executions?definitionId=workflow-def-id
GET /api/v1/workflow-executions?status=RUNNING
```

#### Cancel Execution
```http
POST /api/v1/workflow-executions/{executionId}/cancel
```

## WebSocket Event Streaming

### Connect to WebSocket

Connect to: `ws://localhost:8080/ws`

### Subscribe to All Workflow Events
```javascript
const client = Stomp.over(new SockJS('http://localhost:8080/ws'));

client.connect({}, function() {
    // Subscribe to all workflow events
    client.subscribe('/topic/workflow-events', function(message) {
        const event = JSON.parse(message.body);
        console.log('Workflow event:', event);
    });
});
```

### Subscribe to Specific Execution Events
```javascript
const executionId = 'execution-id-here';
client.subscribe(`/topic/workflow-events/${executionId}`, function(message) {
    const event = JSON.parse(message.body);
    console.log('Execution event:', event);
});
```

### Event Types

- `WORKFLOW_STARTED`: Workflow execution has started
- `WORKFLOW_COMPLETED`: Workflow completed successfully
- `WORKFLOW_FAILED`: Workflow execution failed
- `STEP_STARTED`: A workflow step has started
- `STEP_COMPLETED`: A workflow step completed
- `STEP_FAILED`: A workflow step failed
- `STATE_CHANGED`: Workflow state changed

### Event Message Format
```json
{
  "executionId": "exec-123",
  "caseId": "case-456",
  "workflowDefinitionId": "workflow-def-789",
  "eventType": "WORKFLOW_STARTED",
  "message": "Workflow started",
  "data": {},
  "timestamp": "2024-01-01T10:00:00"
}
```

## Configuration

### Database Configuration

For production, use PostgreSQL. Update `application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/unite
    username: unite
    password: your-password
```

Run with production profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Async Execution Configuration

Adjust thread pool settings in `AsyncConfig.java`:
- `corePoolSize`: Minimum number of threads
- `maxPoolSize`: Maximum number of threads
- `queueCapacity`: Queue capacity for pending tasks

## Integration with workflow-springboot-starter

To integrate the actual workflow engine:

1. Build and install the `workflow-springboot-starter` dependency locally
2. Uncomment the dependency in `pom.xml`
3. Uncomment workflow configuration in `application.yml`
4. Replace the simulated execution logic in `WorkflowExecutionService` with actual RuntimeService calls

## Example Workflow Definition

```json
{
  "name": "order-fulfillment",
  "description": "Complete order fulfillment process",
  "version": "1.0.0",
  "definitionJson": {
    "steps": [
      {
        "id": "validate-order",
        "name": "Validate Order",
        "type": "validation",
        "next": "check-inventory"
      },
      {
        "id": "check-inventory",
        "name": "Check Inventory",
        "type": "inventory-check",
        "next": "process-payment"
      },
      {
        "id": "process-payment",
        "name": "Process Payment",
        "type": "payment",
        "next": "ship-order"
      },
      {
        "id": "ship-order",
        "name": "Ship Order",
        "type": "shipping",
        "next": "end"
      }
    ]
  },
  "active": true
}
```

## Architecture

```
├── config/           # Configuration classes (WebSocket, Async, Jackson)
├── controller/       # REST controllers
├── dto/              # Data Transfer Objects
├── exception/        # Custom exceptions and global handler
├── model/            # JPA entities
├── repository/       # Spring Data repositories
├── service/          # Business logic
└── websocket/        # WebSocket controllers
```

## Testing

Run tests:
```bash
mvn test
```

## Health Check

```http
GET /actuator/health
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## References

- [workflow-springboot-starter](https://github.com/A-N-O-D-E-R/workflow-springboot-starter)
- [workflow](https://github.com/A-N-O-D-E-R/workflow)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
