# Exam Review & Revaluation Management System

A Spring Boot web application that manages the full lifecycle of university exam paper reviews and revaluations — from student application through evaluator marking to final result publication.

---

## Overview

Students can apply for a paper review or full revaluation after results are published. The system enforces strict state transitions at every step, processes payments, assigns evaluators and revaluators, and notifies students at each stage. Administrators and staff have dedicated dashboards for managing the workflow end-to-end.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA |
| Database | PostgreSQL 16 |
| Frontend | Thymeleaf, HTML/CSS/JS (no frontend framework) |
| Build | Maven |
| Containerization | Docker, Docker Compose |

---

## Architecture & Design Patterns

The system is structured in three horizontal layers — controllers, a facade/service layer, and repositories — with GoF design patterns applied deliberately at each boundary. Controllers are intentionally thin: they parse HTTP input, delegate entirely to a service or facade, and return a response. No business logic, state transitions, or direct repository calls appear in any controller class.

Eight Gang-of-Four patterns are implemented across the codebase:

| Pattern | Category | Primary Class(es) |
|---|---|---|
| Factory Method | Creational | `UserFactory` |
| Builder | Creational | `ReviewRequestBuilder` |
| Singleton | Creational | `PaymentGatewaySingleton`, `NotificationService` |
| Abstract Factory | Creational | `PaymentProcessorAbstractFactory` |
| Adapter | Structural | `PaymentGatewayAdapter` |
| Decorator | Structural | `PaymentLoggingDecorator` |
| Facade | Structural | `ExamReviewFacade` |
| Proxy | Structural | `PaymentProxy` |
| State | Behavioral | `AnswerScriptStateMachine`, `ReviewRequestStateMachine`, `RevaluationRequestStateMachine` |
| Observer | Behavioral | `NotificationService`, `NotificationLogger` |
| Chain of Responsibility | Behavioral | `PaymentValidationHandler` + four concrete handlers |
| Strategy | Behavioral | `FeeCalculationStrategy`, `ReviewFeeStrategy`, `FullRevaluationFeeStrategy` |

---

### Creational Patterns

**Factory Method — `UserFactory`**

The system has four distinct user types (`Student`, `Evaluator`, `Revaluator`, `Admin`), all extending a common `User` base class via JPA JOINED inheritance. Rather than scattering `new Student()` / `new Evaluator()` calls across controllers, all instantiation is centralised in `UserFactory.createUser(role, name, email, password)`. The method switches on the role string, instantiates the correct subclass, sets common fields (`name`, `email`, `password`, `role`), and returns a `User` reference. `AuthController` calls the factory and then sets role-specific fields on the returned object (e.g., USN and section for students, department for evaluators). Adding a new user type requires a single new `case` in the factory — no controller changes needed.

**Builder — `ReviewRequestBuilder`**

`ReviewRequest` has four required fields that must all be set consistently before the entity is persisted. Without a builder, the constructor signature grows fragile and the call site becomes unreadable. `ReviewRequestBuilder` provides a fluent API: `withStudent()`, `withAnswerScript()`, `withReviewFee()`, `withReviewStatus()`, then `build()`. `ReviewService.applyForReview()` uses it exclusively, making the construction intent explicit and ensuring every `ReviewRequest` is assembled through the same controlled path. The builder separates the construction logic from the domain class itself, keeping `ReviewRequest` a clean data entity.

**Singleton — `PaymentGatewaySingleton` and `NotificationService`**

Both classes must exist as exactly one instance for the application lifetime.

`PaymentGatewaySingleton` uses the classic approach: private constructor, `private static PaymentGatewaySingleton instance`, and a `public static synchronized getInstance()` method with lazy initialisation. Only one gateway connection object is ever created, preventing duplicate connections in a concurrent environment.

`NotificationService` takes the Spring-idiomatic route: `@Service` singleton scope guarantees a single bean, and a `@PostConstruct` method stores `this` into a `static volatile` field so that non-Spring code (such as static utility paths) can also access it via `NotificationService.getInstance()`. Double-checked locking keeps both implementations thread-safe. The private-constructor trick is intentionally omitted from `NotificationService` because Spring uses CGLIB to subclass `@Service` beans for proxying; a private constructor would cause `BeanCreationException` at startup.

**Abstract Factory — `PaymentProcessorAbstractFactory`**

The factory hierarchy has two levels. `PaymentProcessorAbstractFactory` is the abstract factory interface declaring a single method, `createPaymentProcessor()`. Two concrete factories implement it: `FullPaymentProcessorFactory` creates a `FullPaymentProcessor` and `PartialPaymentProcessorFactory` creates a `PartialPaymentProcessor`. `PaymentProcessorFactory` is the Spring `@Component` that acts as the client: it maintains a `Map<String, PaymentProcessorAbstractFactory>` registry keyed on `"FULL"` and `"PARTIAL"`, and `getPaymentProcessor(type)` delegates to the registered concrete factory. `PaymentService` autowires the factory bean and never references a concrete processor class. Adding an instalment payment mode requires only a new factory class and a new processor class — zero changes to `PaymentService`.

---

### Structural Patterns

**Facade — `ExamReviewFacade`**

`StudentController` imports exactly one collaborator: `ExamReviewFacade`. The facade owns every student-facing operation — fetching results, applying for a review, paying for a review, applying for a revaluation, paying for a revaluation, cancelling requests, and reading notifications — and internally coordinates `IReviewService`, `IRevaluationService`, `PaymentService`, and `IScriptService`. Without the facade, a controller building a dashboard summary would call three or four services individually, creating multiple direct coupling points. The facade collapses that into a single, cohesive method call, keeps `StudentController` ignorant of inter-service interactions, and creates a single place to trace the full student workflow.

**Adapter — `PaymentGatewayAdapter`**

The simulated external payment gateway is a third-party class that cannot be modified. It exposes `charge(double amount, String currency)` — incompatible with the internal `IPaymentGateway` contract that expects `processTransaction(Float amount)`. `PaymentGatewayAdapter` implements `IPaymentGateway`, holds a reference to `ExternalPaymentGateway`, and in `processTransaction` converts `Float` to `double`, supplies `"INR"` as the currency, and maps the returned transaction ID (non-null = success, null = failure) back to a `boolean`. `PaymentGatewayService` can switch between the singleton and the external gateway via `switchToExternalGateway()` / `switchToInternalGateway()` at any time, and no caller is aware of the change. Swapping to a different external provider requires only a new adapter class.

**Decorator — `PaymentLoggingDecorator`**

Audit logging should not be baked into the gateway itself, because the same requirement applies whether the underlying gateway is the singleton, the adapter, or any future implementation. `PaymentLoggingDecorator` implements `IPaymentGateway`, accepts any `IPaymentGateway` in its constructor, and adds timestamped log lines immediately before and after delegating to `processTransaction()`. In `PaymentService.init()`, the decoration stack is assembled programmatically:

```
PaymentProxy (guards) → PaymentLoggingDecorator (logs) → PaymentGatewaySingleton (executes)
```

Callers only ever see `IPaymentGateway`. The layering is entirely transparent to them, and additional decorators (e.g., `MetricsDecorator`, `RetryDecorator`) can be stacked without modifying any existing class — a direct expression of the Open/Closed Principle.

**Proxy — `PaymentProxy`**

This is a Protection Proxy. Before a transaction is forwarded to the real gateway, two guards must pass: the payment amount must be greater than zero, and a student context must be present (proving the student exists and is set on the proxy). `PaymentProxy` implements `IPaymentGateway`. `PaymentService.init()` wires the proxy as the inner layer of the decorator stack, and `PaymentService.processPayment()` calls `paymentProxy.setStudent(student)` before the validation chain runs, giving the proxy the context it needs. If either guard fails, a `RuntimeException` is thrown immediately and neither the decorator nor the real gateway is ever reached. This shields the expensive gateway from malformed or fraudulent requests, and keeps the gateway itself entirely free of validation logic.

---

### Behavioral Patterns

**State — Three State Machines**

Status changes in this domain are not free-form. A script cannot jump from `SUBMITTED` to `FINALIZED`; a review request cannot move from `VERIFIED` back to `PAYMENT_PENDING`. Three dedicated state machine classes encode all permitted transitions as a `HashSet<String>` of `"FROM->TO"` keys:

- **`AnswerScriptStateMachine`** — 15+ transitions covering the full lifecycle: evaluation → review → student decision → revaluation → finalisation.
- **`ReviewRequestStateMachine`** — 6 states: `PAYMENT_PENDING` through `VERIFIED`, `REJECTED`, and `CANCELLED`.
- **`RevaluationRequestStateMachine`** — 8 states: `PAYMENT_PENDING` through `REVALUATION_COMPLETED`, `REJECTED`, and `CANCELLED`.

Every service method that must change a status calls the relevant state machine's static `transition(entity, newStatus)` method. If the transition is not in the allowed set, `InvalidStateTransitionException` is thrown before the entity is touched, let alone saved. No service class ever calls `entity.setStatus()` directly — all status writes in the codebase go through a state machine. This centralises the transition table so that adding a new state requires editing one place, rather than hunting for scattered `if-else` chains across multiple services.

**Observer — `NotificationService` as Subject**

Every state transition should automatically notify the affected student and trigger any registered listeners. `NotificationService` maintains a `List<NotificationListener>` and exposes `addListener()` / `removeListener()`. `ReviewService` calls `notifyReviewStatusChange(request)` and `RevaluationService` calls `notifyRevaluationStatusChange(request)` on every state transition. Each call does three things: persists a `Notification` row to the database (via `NotificationRepository`), logs to console, and fires the corresponding event method on every registered listener.

`NotificationLogger` is a concrete listener that registers itself via `@PostConstruct` and writes structured timestamped log lines. The notification system is fully decoupled from domain logic — adding email delivery or push notifications means implementing `NotificationListener` and registering it, with zero changes to `ReviewService` or `RevaluationService`.

**Strategy — `FeeCalculationStrategy`**

Fee amounts are a business rule that must be changeable without touching service logic. `FeeCalculationStrategy` declares one method: `calculateFee()`. `ReviewFeeStrategy` returns ₹500 and is registered as the Spring bean `"reviewFeeStrategy"`. `FullRevaluationFeeStrategy` returns ₹1500 and is registered as `"fullRevaluationFeeStrategy"`. Services inject the correct strategy by name using `@Qualifier`. To change a fee, only the relevant strategy class needs updating. To introduce a new fee type (e.g., late-submission surcharge), a new implementing class is added — no existing class is modified. This is a direct expression of the Open/Closed Principle applied to fee calculation.

**Chain of Responsibility — Payment Validation**

Before any payment is forwarded to the gateway, it must pass four sequential validation steps. Each step is a concrete subclass of the abstract `PaymentValidationHandler`, which defines `setNext()` and an abstract `handle(payment, userRepository)`. Each handler either throws on failure or calls `handleNext()` to pass control to the next link. The chain is assembled in `PaymentService.init()`:

1. **`AmountValidationHandler`** — rejects amounts that are null or ≤ 0.
2. **`StudentExistsValidationHandler`** — loads the full `Student` entity from the database; rejects if not found, and replaces the lightweight reference on the payment with the fully-loaded entity.
3. **`ScriptStatusValidationHandler`** — verifies the student has an active `PAYMENT_PENDING` review or revaluation request matching the payment type (`PARTIAL` for review, `FULL` for revaluation). Blocks payments that have no corresponding pending request.
4. **`GatewayValidationHandler`** — final pre-flight check; rejects amounts exceeding the ₹1,00,000 system limit and confirms all validations have passed before the gateway is contacted.

Only if all four handlers pass does `PaymentService` proceed to the processor and gateway. Handlers are individually swappable, reorderable, and extendable — disabling `ScriptStatusValidationHandler` for administrative overrides, for example, requires no changes to any other handler or to `PaymentService`.

---

### Design Principles

**Single Responsibility Principle**

Each class has one reason to change. `UserFactory` only creates users — no persistence, validation, or HTTP handling. `NotificationService` only creates and delivers notifications — no payment logic. `PaymentGatewayService` only selects and delegates to a gateway. Every controller handles HTTP for exactly one actor. Every repository handles database access for exactly one entity type. `NotificationLogger` is deliberately separated from `NotificationService` so that delivery and logging can evolve independently.

**Open / Closed Principle**

New behaviour is added by writing new classes, not editing existing ones. `FeeCalculationStrategy` makes new fee types extensions. `PaymentProcessor` makes new payment modes extensions. `PaymentValidationHandler` makes new validation rules extensions. `PaymentLoggingDecorator` makes new cross-cutting gateway concerns extensions.

**Liskov Substitution Principle**

`Student`, `Evaluator`, `Revaluator`, and `Admin` are fully substitutable for `User` — any repository query returning a `User` can be safely cast to the correct subtype, and JPA JOINED inheritance preserves each subtype's contract in separate tables. `FullPaymentProcessor` and `PartialPaymentProcessor` are fully substitutable for `PaymentProcessor`. `PaymentGatewayAdapter`, `PaymentProxy`, and `PaymentGatewaySingleton` are all fully substitutable for `IPaymentGateway` — `PaymentService` calls `processTransaction(Float)` through the interface without caring which concrete class is present.

**Dependency Inversion Principle**

High-level modules depend on abstractions, not concretions. Four service interfaces — `IReviewService`, `IRevaluationService`, `IEvaluatorService`, `IScriptService` — define all business operation contracts. Every controller and the facade declare their service fields as the interface type:

```java
@Autowired private IReviewService      reviewService;
@Autowired private IRevaluationService revaluationService;
@Autowired private IEvaluatorService   evaluatorService;
@Autowired private IScriptService      scriptService;
```

Spring injects the concrete implementation at startup. `PaymentService` depends on `IPaymentGateway`, not on any specific gateway class. Swapping the entire gateway implementation requires zero changes to `PaymentService`. The Spring IoC container itself enforces this principle application-wide — no high-level orchestrator ever calls `new ConcreteService()`.

---

## User Roles

| Role | Responsibilities |
|---|---|
| **Student** | View results, apply for review/revaluation, make payments, track status |
| **Evaluator** | Mark assigned scripts, publish results |
| **Revaluator** | Submit revaluation marks on assigned requests |
| **Admin** | Assign evaluators/revaluators, manage review and revaluation requests, publish and finalize results, manage users |

---

## State Machine

### Answer Script

```
SUBMITTED → UNDER_EVALUATION → EVALUATED → RESULTS_PUBLISHED
RESULTS_PUBLISHED → REVIEW_REQUESTED → REVIEW_PAYMENT_PENDING → REVIEW_IN_PROGRESS
REVIEW_IN_PROGRESS → REVIEW_COMPLETED → AWAIT_STUDENT_DECISION
AWAIT_STUDENT_DECISION → FINALIZED                    (student accepts)
AWAIT_STUDENT_DECISION → REVALUATION_REQUESTED        (student escalates)
REVALUATION_REQUESTED → REVALUATION_PAYMENT_PENDING → REVALUATION_IN_PROGRESS
REVALUATION_IN_PROGRESS → REVALUATION_COMPLETED → FINAL_RESULT_UPDATED → FINALIZED
```

### Review Request

```
PAYMENT_PENDING → REVIEW_REQUESTED → UNDER_REVIEW → REVIEW_COMPLETED → VERIFIED
PAYMENT_PENDING → PAYMENT_FAILED / CANCELLED
```

### Revaluation Request

```
PAYMENT_PENDING → REVALUATION_IN_PROGRESS → REVALUATION_COMPLETED
PAYMENT_PENDING → PAYMENT_FAILED / CANCELLED
```

---

## API Endpoints

### Student (`/student`)

| Method | Path | Description |
|---|---|---|
| GET | `/results/{studentId}` | Fetch all answer scripts for a student |
| POST | `/review/apply` | Create a review request (status: `PAYMENT_PENDING`) |
| POST | `/review/{reviewId}/pay` | Process payment → `REVIEW_REQUESTED` |
| GET | `/review/student/{studentId}` | List student's review requests |
| POST | `/revaluation/apply` | Create a revaluation request (status: `PAYMENT_PENDING`) |
| POST | `/revaluation/{id}/pay` | Process payment → `REVALUATION_IN_PROGRESS` |
| GET | `/revaluation/student/{studentId}` | List student's revaluation requests |

### Evaluator (`/evaluator`)

| Method | Path | Description |
|---|---|---|
| GET | `/scripts/pending` | Scripts with status `UNDER_EVALUATION` |
| PUT | `/scripts/{scriptId}/submit` | Submit marks → `EVALUATED` |
| PUT | `/scripts/{scriptId}/verify` | Publish results → `RESULTS_PUBLISHED` |

### Revaluator (`/revaluator`)

| Method | Path | Description |
|---|---|---|
| GET | `/requests` | Pending requests (`REVALUATION_IN_PROGRESS`) |
| PUT | `/requests/{id}/submit` | Submit revaluation marks → `REVALUATION_COMPLETED` |

### Admin (`/admin`)

| Method | Path | Description |
|---|---|---|
| POST | `/evaluator/assign` | Assign evaluator to script → `UNDER_EVALUATION` |
| POST | `/revaluator/assign` | Assign revaluator to request |
| GET/PUT | `/reviews/*` | Manage review requests |
| GET/PUT | `/revaluations/*` | Manage revaluation requests |
| PUT | `/results/{scriptId}/publish` | Publish result → `RESULTS_PUBLISHED` |
| PUT | `/results/{scriptId}/finalize` | Finalize result → `FINALIZED` |
| GET/DELETE | `/users/*` | User management |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose

### Run with Docker Compose

```bash
git clone <repository-url>
cd revaluation
docker compose up --build
```

The application will be available at `http://localhost:8080`.

The database is initialised automatically via `spring.jpa.hibernate.ddl-auto=update`.

### Run Locally (without Docker)

Requires Java 21 and a running PostgreSQL instance.

```bash
# Set environment variables or edit application.properties
export DB_URL=jdbc:postgresql://localhost:5432/revaluation_db
export DB_USERNAME=postgres
export DB_PASSWORD=psql123

mvn spring-boot:run
```

---

## Project Structure

```
src/main/java/com/team/revaluation/
├── builder/          # ReviewRequestBuilder (Builder pattern)
├── controller/       # REST controllers (thin, delegate to Facade/services)
├── exception/        # InvalidStateTransitionException
├── facade/           # ExamReviewFacade (Facade pattern)
├── factory/          # UserFactory, PaymentProcessorFactory, AbstractFactory
├── model/            # JPA entities (User hierarchy, AnswerScript, etc.)
├── repository/       # Spring Data JPA repositories
└── service/
    ├── *StateMachine.java     # State pattern
    ├── *Strategy.java         # Strategy pattern
    ├── Payment*.java          # Proxy, Decorator, Adapter, Chain of Responsibility
    ├── NotificationService.java  # Singleton + Observer subject
    └── NotificationLogger.java   # Observer listener
```

---

## Default Credentials

No seed data is included. Register users via `/register` or the Admin → User Management panel. All four roles (Student, Evaluator, Revaluator, Admin) can be created through the registration form.

---

## Logs

Application logs are written to `logs/revaluation.log` with daily rotation (30-day retention), configured in `logback-spring.xml`. The log directory is excluded from Docker images via `.dockerignore`.