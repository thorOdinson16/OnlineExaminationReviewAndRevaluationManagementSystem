# 📋 Online Examination Review & Revaluation Management System

A full-stack web application built with **Spring Boot** and **PostgreSQL** that manages the complete lifecycle of exam answer script evaluation, student-initiated reviews, and revaluations — from mark submission to final result. The project is a showcase of classical **Gang of Four design patterns** applied rigorously throughout a real-world domain.

![Java](https://img.shields.io/badge/Java-17-orange?logo=java) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.3-green?logo=springboot) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-UI-teal)

---

## ✨ Features

- **Role-Based Access** — four distinct roles: Student, Evaluator, Revaluator, and Admin, each with their own dashboard and capabilities
- **Strict State Machines** — answer scripts, review requests, and revaluation requests each follow enforced state transition graphs; illegal transitions throw a typed exception
- **End-to-End Review Flow** — students can apply for a paper review, pay the fee, view the reviewed paper, and choose to accept or escalate to a full revaluation
- **Payment Pipeline** — chain-of-responsibility validation, proxy authorization, logging decorator, and an adapter to an external gateway
- **Notifications** — students receive in-app notifications at every significant status change (Observer pattern)
- **Seeded Demo Data** — a `DataLoader` seeds sample users, exams, and scripts on first boot so the app is immediately explorable
- **Structured Logging** — rolling file logs via Logback with a 30-day retention policy

---

## 🏗️ Architecture & Design Patterns

The codebase is a deliberate catalog of GoF patterns. Here's where each one lives:

| Pattern | Category | Location |
|---|---|---|
| **Builder** | Creational | `ReviewRequestBuilder` — fluent construction of `ReviewRequest` objects |
| **Factory Method** | Creational | `UserFactory` — creates `Student`, `Evaluator`, `Revaluator`, or `Admin` from a role string |
| **Abstract Factory** | Creational | `PaymentProcessorAbstractFactory` — produces `FullPaymentProcessor` or `PartialPaymentProcessor` |
| **Singleton** | Creational | `PaymentGatewaySingleton` — single shared gateway connection |
| **Facade** | Structural | `ExamReviewFacade` — `StudentController` uses this as its sole entry point to all services |
| **Adapter** | Structural | `PaymentGatewayAdapter` — adapts `ExternalPaymentGateway` to the internal `IPaymentGateway` interface |
| **Proxy** | Structural | `PaymentProxy` — authorization check before delegating to the real payment service |
| **Decorator** | Structural | `PaymentLoggingDecorator` — wraps a `PaymentProcessor` to add structured logging |
| **State Machine** | Behavioral | `AnswerScriptStateMachine`, `ReviewRequestStateMachine`, `RevaluationRequestStateMachine` |
| **Strategy** | Behavioral | `ReviewFeeStrategy`, `FullRevaluationFeeStrategy`, `FeeCalculationStrategy` — swappable fee calculation |
| **Observer** | Behavioral | `NotificationService` + `NotificationLogger` — notifies students on status changes |
| **Chain of Responsibility** | Behavioral | `AmountValidationHandler` → `StudentExistsValidationHandler` → `ScriptStatusValidationHandler` → `GatewayValidationHandler` → `PaymentValidationHandler` |

### Answer Script State Machine

```
SUBMITTED → UNDER_EVALUATION → EVALUATED → RESULTS_PUBLISHED
                                                    │
                                            REVIEW_REQUESTED
                                                    │
                                       REVIEW_PAYMENT_PENDING
                                                    │
                                          REVIEW_IN_PROGRESS
                                                    │
                                          REVIEW_COMPLETED
                                                    │
                                       AWAIT_STUDENT_DECISION
                                          /              \
                                    FINALIZED     REVALUATION_REQUESTED
                                                          │
                                              REVALUATION_PAYMENT_PENDING
                                                          │
                                              REVALUATION_IN_PROGRESS
                                                          │
                                              REVALUATION_COMPLETED
                                                          │
                                              FINAL_RESULT_UPDATED → FINALIZED
```

Any transition not in the table above throws `InvalidStateTransitionException`.

---

## 👥 User Roles & Capabilities

**Student**
- View exam results and answer scripts
- Apply for a paper review and pay the review fee
- View the reviewed paper and decide to accept or escalate to revaluation
- Apply for revaluation and pay the revaluation fee
- Track request status and receive notifications

**Evaluator**
- View scripts assigned for evaluation (`UNDER_EVALUATION`)
- Submit marks → transitions script to `EVALUATED`
- Verify/publish results → transitions to `RESULTS_PUBLISHED`

**Revaluator**
- View revaluation requests assigned to them
- Submit revaluation marks and complete the revaluation

**Admin**
- Full oversight: view all users, scripts, reviews, and revaluations
- Assign evaluators to scripts and revaluators to revaluation requests
- Verify and advance requests through the workflow

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Database Setup

```sql
CREATE DATABASE revaluation_db;
```

### Configuration

The app reads database credentials from environment variables with sensible defaults:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/revaluation_db
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

Or edit `src/main/resources/application.properties` directly.

### Run

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**. On first boot, `DataLoader` seeds the following demo accounts:

| Role | Email | Password |
|---|---|---|
| Admin | admin@example.com | admin123 |
| Student | john@example.com | password |
| Student | jane@example.com | password |
| Evaluator | robert@example.com | password |
| Revaluator | emily@example.com | password |

### Build JAR

```bash
mvn clean package
java -jar target/revaluation-0.0.1-SNAPSHOT.jar
```

---

## 📡 API Reference

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user (role: STUDENT / EVALUATOR / REVALUATOR / ADMIN) |
| POST | `/auth/login` | Login |

### Student (`/student`)
| Method | Path | Description |
|---|---|---|
| GET | `/student/results/{studentId}` | Get all answer scripts for a student |
| POST | `/student/review/apply` | Apply for a paper review (creates `PAYMENT_PENDING` request) |
| POST | `/student/review/{reviewId}/pay` | Pay review fee → `REVIEW_REQUESTED` |
| GET | `/student/review/{reviewId}` | Get review details |
| POST | `/student/revaluation/apply` | Apply for revaluation |
| POST | `/student/revaluation/{id}/pay` | Pay revaluation fee → `REVALUATION_IN_PROGRESS` |
| DELETE | `/student/revaluation/{id}/cancel` | Cancel a revaluation request |
| GET | `/student/notifications/{studentId}` | Get unread notifications |
| PUT | `/student/notifications/{id}/read` | Mark notification as read |

### Evaluator (`/evaluator`)
| Method | Path | Description |
|---|---|---|
| GET | `/evaluator/scripts/pending` | Get scripts in `UNDER_EVALUATION` |
| PUT | `/evaluator/scripts/{scriptId}/submit` | Submit marks → `EVALUATED` |
| PUT | `/evaluator/scripts/{scriptId}/verify` | Publish results → `RESULTS_PUBLISHED` |

### Revaluator (`/revaluator`)
| Method | Path | Description |
|---|---|---|
| GET | `/revaluator/requests` | Get assigned revaluation requests |
| PUT | `/revaluator/requests/{id}/complete` | Submit revaluation result |

### Admin (`/admin`)
| Method | Path | Description |
|---|---|---|
| GET | `/admin/reviews` | All review requests |
| GET | `/admin/reviews/pending` | Reviews in `PAYMENT_PENDING` |
| PUT | `/admin/reviews/{reviewId}/verify` | Verify a review |
| POST | `/admin/evaluator/assign` | Assign evaluator to a script |
| GET | `/admin/revaluations` | All revaluation requests |
| POST | `/admin/revaluator/assign` | Assign revaluator to a request |
| GET | `/admin/users` | All registered users |

---

## 🗂️ Project Structure

```
src/main/java/com/team/revaluation/
├── builder/            # Builder pattern — ReviewRequestBuilder
├── config/             # DataLoader (seeds demo data on first boot)
├── controller/         # REST + page controllers (one per role)
├── exception/          # InvalidStateTransitionException
├── facade/             # ExamReviewFacade (Facade pattern)
├── factory/            # UserFactory, PaymentProcessorAbstractFactory
├── model/              # JPA entities: User, Student, Evaluator, Exam, AnswerScript, ...
├── repository/         # Spring Data JPA repositories
└── service/            # All business logic, state machines, strategies, payment pipeline
src/main/resources/
├── templates/          # Thymeleaf HTML views (per-role dashboards + flows)
└── application.properties
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Templating | Thymeleaf |
| Build | Maven |
| Utilities | Lombok |
| Logging | Logback (rolling file, 30-day retention) |

---

## 📄 License

MIT — see [LICENSE](LICENSE) for details.
