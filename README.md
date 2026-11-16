
# E-commerce Seller Management Platform

A full-stack seller management platform for managing product catalogues, inventory, pricing, promotions, customer orders and loyalty points.

The project focuses on backend engineering, transactional consistency, distributed event processing, security and maintainable application architecture.

## Technology Stack

- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA
- **Database:** PostgreSQL
- **Caching:** Redis
- **Messaging:** Apache Kafka
- **Frontend:** React, TypeScript
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Infrastructure:** Docker, Docker Compose, GitHub Actions

## Core Modules

- Identity and Access Management
- Product Management
- Inventory Management
- Pricing and Promotions
- Order Management
- Loyalty Points
- Seller Analytics
- Event Processing

## Engineering Focus

- Transactional inventory management
- Optimistic locking and concurrent stock updates
- Idempotent order processing
- Transactional Outbox Pattern
- Kafka retries and dead-letter topics
- Redis caching and invalidation
- Seller-level data isolation
- Automated integration and concurrency testing
- Application observability

## Architecture

The application follows a modular monolith architecture with asynchronous event processing.

PostgreSQL is the primary source of truth, while Redis provides selected caching capabilities and Kafka supports asynchronous business events.

See `docs/architecture/overview.md` for the architecture overview.

## Project Structure

- `backend/` — Spring Boot application
- `frontend/` — React and TypeScript application
- `infrastructure/` — Docker and Kafka configuration
- `docs/` — Architecture and engineering documentation
- `scripts/` — Development utilities
- `.github/workflows/` — CI/CD workflows

## Development Status

Initial repository setup completed. Application modules will be implemented incrementally, with features verified through automated tests before being marked complete.
