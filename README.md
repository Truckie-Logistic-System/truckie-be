<div align="center">

# 🚛 Truckie Backend

### Enterprise Logistics & Fleet Management System

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Railway](https://img.shields.io/badge/Railway-Deployed-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)](https://railway.app/)

*A comprehensive backend solution for truck logistics operations, featuring real-time tracking, intelligent route optimization, automated order management, and AI-powered customer support.*

[Live Demo](https://truckie.vercel.app/) • [API Documentation](https://web-production-7b905.up.railway.app/swagger-ui/index.html) • [Report Bug](#-contributing) • [Request Feature](#-contributing)

</div>

---

## 📋 Table of Contents

- [About The Project](#-about-the-project)
- [Key Features](#-key-features)
- [Tech Stack](#️-tech-stack)
- [System Architecture](#-system-architecture)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Database Schema](#️-database-schema)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 About The Project

**Truckie** is a full-featured logistics management platform developed as a capstone project at FPT University (Fall 2025). The system digitizes and streamlines truck transportation operations, connecting shippers with carriers while providing comprehensive fleet management capabilities.

### 🎓 Capstone Project Details
- **University:** FPT University
- **Semester:** Fall 2025
- **Team Size:** 5 members
- **Development Duration:** 4 months

### 💡 Problem Statement

Traditional logistics operations in Vietnam face challenges including:
- Inefficient manual order processing
- Lack of real-time shipment visibility
- Difficulty in route optimization
- Poor communication between stakeholders
- Complex pricing and billing management

### ✅ Our Solution

Truckie provides an end-to-end digital platform that automates logistics workflows, enables real-time tracking, and optimizes operations through intelligent algorithms.

---

## ⭐ Key Features

### 📦 Order Management
- **Real-time order tracking** - GPS-based location updates via WebSocket
- **Automated status transitions** - Smart order lifecycle management
- **Digital contracts** - PDF contract generation with e-signatures
- **Order confirmation system** - Photo verification and digital signatures

### 🚚 Fleet Management
- **Vehicle registration & tracking** - Complete vehicle lifecycle management
- **Driver assignment** - Intelligent driver-vehicle matching
- **Maintenance scheduling** - Automated service reminders and records
- **License expiry alerts** - Proactive notification system
- **Vehicle reservation system** - Advance booking for vehicles

### 🗺️ Route & Navigation
- **Real-time GPS tracking** - Live vehicle location monitoring
- **Route optimization** - Intelligent path calculation using Vietmap
- **Off-route detection** - Automated deviation alerts with grace periods
- **Journey history** - Complete trip records and analytics

### 💰 Pricing & Payments
- **Dynamic pricing engine** - Distance-based and weight-based calculations
- **Multiple payment gateways** - PayOS integration

### 🤖 AI-Powered Features
- **Intelligent chatbot** - AI customer support using knowledge base
- **Automated responses** - FAQ handling and query resolution
- **Smart notifications** - Context-aware push notifications

### 🔐 Security & Authentication
- **JWT-based authentication** - Secure token management
- **Role-based access control** - Admin, Staff, Driver, Customer roles
- **Rate limiting** - DDoS protection with Bucket4j

### 📊 Analytics & Reporting
- **Admin dashboard** - Comprehensive business analytics
- **Driver performance metrics** - Efficiency and reliability scores
- **Financial reports** - Revenue and expense tracking

---

## 🛠️ Tech Stack

### Backend Framework
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 LTS | Core programming language |
| Spring Boot | 3.3.x | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.x | Data persistence |
| Spring WebSocket | 6.x | Real-time communication |
| Spring WebFlux | 6.x | Reactive programming |

### Database & Caching
| Technology | Purpose |
|------------|---------|
| PostgreSQL 14 | Primary database |
| Liquibase | Database migration & versioning |
| Spring Cache | Application-level caching |

### Cloud & Infrastructure
| Technology | Purpose |
|------------|---------|
| Docker | Containerization |
| Railway | Backend & Database deployment |
| Cloudinary | Image/file storage |

### External Integrations
| Service | Purpose |
|---------|---------|
| Vietmap | Route calculation & geocoding |
| PayOS | Vietnam payment gateway |

### Development Tools
| Tool | Purpose |
|------|---------|
| Gradle (Kotlin DSL) | Build automation |
| MapStruct | Object mapping |
| Lombok | Boilerplate reduction |
| Swagger/OpenAPI | API documentation |
| iText | PDF generation |

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT APPLICATIONS                             │
├──────────────────┬──────────────────┬──────────────────┬───────────────────┤
│   Web Admin      │   Customer App   │   Driver App     │   Staff Portal    │
│   (React.ts)     │   (React.ts) │   (Flutter Android) │   (React.ts)      │
└────────┬─────────┴────────┬─────────┴────────┬─────────┴─────────┬─────────┘
         │                  │                  │                   │
         └──────────────────┴────────┬─────────┴───────────────────┘
                                     │
                           ┌─────────▼─────────┐
                           │   API Gateway     │
                           │   (Spring Boot)   │
                           └─────────┬─────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
┌────────▼────────┐        ┌─────────▼─────────┐       ┌────────▼────────┐
│  REST API       │        │  WebSocket API    │       │  Authentication |
│  Controllers    │        │  (Real-time)      │       │                 │
└────────┬────────┘        └─────────┬─────────┘       └────────┬────────┘
         │                           │                          │
         └───────────────────────────┼──────────────────────────┘
                                     │
                           ┌─────────▼─────────┐
                           │  Service Layer    │
                           │  (Business Logic) │
                           └─────────┬─────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
┌────────▼────────┐        ┌─────────▼─────────┐       ┌────────▼────────┐
│  Repository     │        │  External APIs    │       │  Event          │
│  (JPA/Postgres) │        │  (Maps, Payment)  │       │  Schedulers     │
└────────┬────────┘        └─────────┬─────────┘       └────────┬────────┘
         │                           │                          
         ▼                           ▼                          
┌─────────────────┐        ┌─────────────────┐       
│   PostgreSQL    │        │   Cloudinary    │       
│   Database      │        │   VietMap       │       
└─────────────────┘        │   PayOS         │       
                           └─────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:
- **Java 17** or higher
- **Gradle 8.x** (or use the included Gradle Wrapper)
- **PostgreSQL 14** or higher
- **Docker** (optional, for containerized deployment)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/truckie-be.git
   cd truckie-be
   ```

2. **Configure the database**
   
   Create a PostgreSQL database:
   ```sql
   CREATE DATABASE truckie;
   ```

3. **Set up environment variables**
   
   Copy the example properties and configure:
   ```bash
   cp src/main/resources/application.properties src/main/resources/application-local.properties
   ```
   
   Update the following properties:
   ```properties
   # Database
   spring.datasource.url=jdbc:postgresql://localhost:5432/truckie
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
   # JWT Secret
   jwt.secret=your_jwt_secret_key
   
   # External Services (optional)
   cloudinary.cloud-name=your_cloud_name
   cloudinary.api-key=your_api_key
   cloudinary.api-secret=your_api_secret
   ```

4. **Run database migrations**
   ```bash
   ./gradlew update -PrunList=main
   ```

5. **Build and run the application**
   ```bash
   # Build
   ./gradlew clean build
   
   # Run
   ./gradlew bootRun
   ```

6. **Access the application**
   - API Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

### Docker Deployment

```bash
# Build the Docker image
docker build -t truckie-backend .

# Run with Docker Compose
cd development
docker-compose up -d
```

---

## 📚 API Documentation

### Base URL
- **Production (Backend):** `https://web-production-7b905.up.railway.app`
- **Production (Frontend):** `https://truckie.vercel.app`
- **Development (Backend):** `http://localhost:8080`
- **Development (Frontend):** `http://localhost:5173`

### API Versioning
All endpoints are prefixed with `/v1.0/`

### Main API Modules

| Module | Endpoint | Description |
|--------|----------|-------------|
| **Authentication** | `/v1.0/auths/*` | Login, register, password reset |
| **Users** | `/v1.0/users/*` | User profile management |
| **Drivers** | `/v1.0/drivers/*` | Driver management & assignments |
| **Orders** | `/v1.0/orders/*` | Order CRUD & lifecycle |
| **Vehicles** | `/v1.0/vehicles/*` | Fleet management |
| **Routes** | `/v1.0/routes/*` | Route planning & optimization |
| **Pricing** | `/v1.0/pricing/*` | Dynamic pricing calculations |
| **Payments** | `/v1.0/payments/*` | Payment processing |
| **Notifications** | `/v1.0/notifications/*` | Push & in-app notifications |
| **Chat** | `/v1.0/chat/*` | AI chatbot integration |
| **Admin** | `/v1.0/admin/*` | Administrative operations |
| **Dashboard** | `/v1.0/dashboard/*` | Analytics & reporting |

### Authentication

All protected endpoints require a Bearer token:
```http
Authorization: Bearer <your_jwt_token>
```

### WebSocket Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/ws/tracking` | Real-time vehicle location updates |
| `/ws/notifications` | Live notification delivery |
| `/ws/orders` | Order status changes |

### Interactive Documentation

Access the full Swagger/OpenAPI documentation:
- **Swagger UI:** [https://web-production-7b905.up.railway.app/swagger-ui/index.html](https://web-production-7b905.up.railway.app/swagger-ui/index.html)

---

## 🗄️ Database Schema

### Core Entities

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Users     │────▶│   Drivers    │────▶│  Vehicles    │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                    │
       ▼                    ▼                    ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Orders     │────▶│ Assignments  │────▶│   Routes     │
└──────────────┘     └──────────────┘     └──────────────┘
       │                                         │
       ▼                                         ▼
┌──────────────┐                         ┌──────────────┐
│ Transactions │                         │  Off-Route   │
│   Contracts  │                         │   Events     │
└──────────────┘                         └──────────────┘
```

### Database Migration

Using Liquibase for version-controlled schema changes:
```bash
# Apply migrations
./gradlew update

# Generate diff changelog
./gradlew diffChangeLog -PrunList=generateDiff

# Rollback last change
./gradlew rollbackCount -PliquibaseCommandValue=1
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Write unit tests for new features
- Update documentation as needed

---

## 📄 License

This project is developed for educational purposes as part of FPT University's Capstone Project program.

<div align="center">

### ⭐ Star this repository if you find it helpful!

**Built with ❤️ by FPT University Students**

</div>
