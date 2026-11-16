# E-commerce Seller Management Platform

A full-stack seller management platform designed to manage product
catalogues, inventory, pricing, promotions, customer orders and loyalty
points.

The project focuses on backend engineering, transactional consistency,
distributed event processing, security and maintainable application
architecture.

## Technology Stack

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- JWT Authentication
- REST APIs

### Database and Messaging
- PostgreSQL
- Redis
- Apache Kafka

### Frontend
- React
- TypeScript

### Testing
- JUnit 5
- Mockito
- Testcontainers

### Infrastructure
- Docker
- Docker Compose
- GitHub Actions
- AWS-ready deployment architecture

## Core Features

- Product catalogue management
- Inventory tracking and stock reservations
- Pricing and promotional campaigns
- Customer order management
- Loyalty points and transaction history
- Seller analytics dashboard
- Role-based access control

## Engineering Focus

- Transactional inventory management
- Optimistic locking and concurrent stock updates
- Idempotent order processing
- Transactional Outbox Pattern
- Kafka retries and dead-letter topics
- Redis caching and invalidation
- Seller-level data isolation
- Automated integration and concurrency testing
- Application logging and observability

## Architecture

The initial implementation will use a modular monolith with
asynchronous event processing.

Business modules will remain logically separated while sharing
a common deployable backend application.

See [Architecture Overview](docs/architecture/overview.md).

## Project Structure

```text
backend/                 Spring Boot application
frontend/                React and TypeScript application
infrastructure/          Docker and Kafka configuration
docs/                    Architecture and engineering documentation
scripts/                 Development and utility scripts
.github/workflows/       CI/CD workflows

## 2. Create `.gitignore`

```bash
cat > .gitignore <<'EOF'
# Java
*.class
*.jar
*.war
*.ear
hs_err_pid*
replay_pid*

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
release.properties

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# Node
node_modules/
dist/
build/
coverage/
npm-debug.log*
yarn-debug.log*
pnpm-debug.log*

# Environment and secrets
.env
.env.*
!.env.example
*.pem
*.key
secrets/

# Operating system
.DS_Store
.AppleDouble
.LSOverride

# Logs
*.log
logs/

# Test and temporary files
.tmp/
temp/
.cache/

# Docker local data
docker-data/
