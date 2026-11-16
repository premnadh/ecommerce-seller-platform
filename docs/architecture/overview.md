# System Architecture Overview

## Architecture Style

The application will initially use a modular monolith architecture.
Business modules will be logically separated within one Spring Boot application.

## Business Modules

- Identity and Access Management
- Product Management
- Inventory Management
- Pricing and Promotions
- Order Management
- Loyalty Points
- Seller Analytics
- Event Processing

## Technology

- Java 21 and Spring Boot
- PostgreSQL
- Redis
- Apache Kafka
- React and TypeScript

## Data Management

PostgreSQL will be the primary source of truth.
Redis will cache selected frequently accessed product information.

## Inventory Consistency

Inventory operations must prevent negative stock.
Optimistic locking and conditional database updates will be evaluated
for concurrent stock operations.

## Event Processing

Kafka will support asynchronous order-related events.
The Transactional Outbox Pattern will provide reliable event publication.

## Security

- JWT authentication
- Secure password hashing
- Seller and administrator roles
- Seller-level resource ownership checks

## Testing

The project will include unit, integration, API and concurrency tests.
Testcontainers will support infrastructure integration testing.

## Architecture Evolution

Significant architectural changes will be documented in
docs/engineering-decisions/.
