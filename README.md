# Smart Mobility Billing Service

## Overview
The **Smart Mobility Billing Service** is a core microservice responsible for managing user accounts, handling financial transactions (top-ups, debits), and enforcing business rules such as daily spending limits. It is a vital part of the Smart Mobility ecosystem, integrating closely with other services via Spring Cloud and RabbitMQ.

## Features
- **Account Management**: Create and manage user billing accounts.
- **Transaction Processing**: Handle credits (top-ups) and debits (trip payments).
- **Daily Spending Limits**: Enforces a configurable daily cap on spending (`billing.daily-cap`).
- **Event-Driven Architecture**: Consumes trip pricing events and publishes payment success/failure events via RabbitMQ.
- **Resilience**: Integrated with Resilience4j for circuit breaking.
- **Observability**: Centralized configuration, service discovery, and distributed tracing.

## Technology Stack
- **Java 17**
- **Spring Boot**
- **Spring Cloud** (Config, Eureka, Resilience4j)
- **PostgreSQL** (Relational Database)
- **RabbitMQ** (Message Broker)
- **Zipkin & Micrometer** (Distributed Tracing & Metrics)
- **Project Lombok** (Boilerplate reduction)

## Architecture
The service follows a standard layered architecture:
- `Controller`: REST endpoints for account operations.
- `Service`: Core business logic (managing accounts, enforcing caps, processing transactions).
- `Repository`: Spring Data JPA interfaces for database access.
- `Messaging`: RabbitMQ publishers and consumers for asynchronous communication.
- `Model`: JPA Entities (`Account`, `Transaction`).
- `DTO`: Data Transfer Objects for REST and Messaging.

## Prerequisites & Infrastructure
To run this service locally, the following infrastructure components are expected:
- **PostgreSQL**: `localhost:5432` (DB: `billing_db`, User: `billing_user`, Pass: `billing_pass`)
- **RabbitMQ**: `localhost:5672` (Guest/Guest)
- **Spring Cloud Config Server**: `http://localhost:8888`
- **Eureka Server**: `http://localhost:8761`
- **Zipkin**: `http://localhost:9411`

## API Endpoints

### Account Management
- `POST /accounts`
  - **Description**: Creates a new account for a given user.
  - **Body**: `{ "userId": 123 }`
- `GET /accounts/{userId}`
  - **Description**: Retrieves account details including current balance.
- `POST /accounts/{userId}/topup`
  - **Description**: Credits the user's account with a specific amount.
  - **Body**: `{ "amount": 1000.0, "description": "Top up via Credit Card" }`

## RabbitMQ Messaging
- **Consumers**:
  - Listens to `TripPricedEvent` to automatically deduct the trip cost from the user's account.
- **Publishers**:
  - Publishes `PaymentEvent` (Success/Failed) after attempting to process a trip payment.

## Running the Application
### Using Maven
```bash
./mvnw clean install
./mvnw spring-boot:run
```
*(Make sure to have your instances for Postgres, RabbitMQ, Eureka, and Config Server running before starting the service)*

## Configuration Highlights
Key application properties (`application.properties`):
```properties
server.port=8083
spring.application.name=smart-mobility-billing-service
billing.daily-cap=50000
```
