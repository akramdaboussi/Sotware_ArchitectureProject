# 🏗️ Architecture Quick Guide

> **TL;DR**: A stateless authentication system built with Spring Boot + H2 Database + JPA. JWT tokens, no server-side session. Learn by doing.

---

## 🎯 What Is This?

A **4-layer authentication API** that handles user registration, login, JWT management, and role-based access:

```
Browser → Controller → Service → Repository → Database
  ↓          ↓           ↓           ↓            ↓
 UI      HTTP/JSON   Business     JPA/H2      Persistent
                      Logic                    Storage
```

---

## 🧱 The 4 Layers Explained

### Layer 1: **Controller** (HTTP Interface)
**Job**: Talk to the outside world  
**Files**: `AuthController.java`  
**Does**: Receive requests → Call service → Return responses  
**Never Does**: Business logic, database access

```java
// Controller = Traffic cop
@PostMapping("/api/auth/login")
public ResponseEntity login(@RequestBody Map request) {
    String email = request.get("email");
    String jwt = authService.login(email, password); // ← Call service
    return ResponseEntity.ok(Map.of("token", jwt));
}
```

### Layer 2: **Service** (Business Brain)
**Job**: Make decisions  
**Files**: `AuthService`, `UserService`  
**Does**: Validate, hash passwords, call JWT utility  
**Never Does**: HTTP stuff, know about JSON

```java
// Service = The brain
public String login(String email, String password) {
    User user = userService.findByEmail(email);      // Find
    if (!verifyPassword(password, user.getPassword())) {  // Check
        throw new RuntimeException("Invalid credentials");
    }
    return jwtUtil.generateToken(user);         // Create JWT (via utils)
}
```

### Layer 3: **Repository** (Data Access)
**Job**: Store and find stuff  
**Files**: `UserRepository.java`, `RoleRepository.java` + models (`User`, `Role`)  
**Does**: Save/retrieve data via JPA, database queries  
**Never Does**: Validation, business rules

```java
// Repository = Database access via JPA
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### Layer 4: **Database** (H2 Persistent Storage)
**Job**: Persist data between restarts  
**Files**: `application.properties`, `./data/authdb`  
**Does**: Store users, roles in tables  
**Console**: Accessible via `/h2-console`

---

## 🔑 How Authentication Works

### Registration: 5 Steps
```mermaid
graph LR
    A[1. POST<br/>/register] --> B[2. Hash<br/>password]
    B --> C[3. Save<br/>user]
    C --> D[4. Create<br/>JWT]
    D --> E[5. Return<br/>201 + JWT]
    
    style A fill:#818cf8
    style B fill:#f59e0b
    style C fill:#10b981
    style D fill:#ec4899
    style E fill:#8b5cf6
```

### Login: 4 Steps
```mermaid
graph LR
    A[1. POST<br/>/login] --> B[2. Find<br/>user]
    B --> C[3. Verify<br/>password]
    C --> D[4. Generate<br/>JWT]
    
    style A fill:#818cf8
    style B fill:#10b981
    style C fill:#f59e0b
    style D fill:#ec4899
```

### Token: JWT Format

JWT (JSON Web Token) is a signed, stateless token containing user info and expiry. It is not stored in the database.

- ✅ Learn how JWT works
- ✅ Production-ready (stateless, secure)

---

## 🤔 Why These Design Choices?

| Choice | Why? | Trade-off |
|--------|------|-----------|
| **JWT tokens** (stateless) | Modern, secure, scalable | No server-side revocation |
| **SHA-256** (not BCrypt) | Built into Java | Less secure |
| **H2 database** | Persistent storage, easy setup | Not production-ready |
| **JPA/Hibernate** | Standard ORM, auto schema | Learning curve |
| **Layered architecture** | Clean, testable, maintainable | More files |
| **Role-based access** | Admin/User separation | More complexity |

---

## 📊 The Complete Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant AuthService
    participant UserService
    participant JwtUtil (utils)
    participant Storage
    
    User->>Controller: POST /login
    Controller->>AuthService: login(email, pwd)
    AuthService->>UserService: findByEmail()
    UserService->>UserRepository: findByEmail()
    UserRepository->>Database: SELECT user
    Database-->>UserRepository: User
    UserRepository-->>UserService: User
    AuthService->>UserService: verifyPassword()
    UserService-->>AuthService: ✅ Valid
    AuthService->>JwtUtil: generateToken()
    JwtUtil-->>AuthService: JWT
    AuthService-->>Controller: token
    Controller-->>User: 200 OK {token}
```

---

## 🔒 Security Reality Check

| Feature | Status | Production Need |
|---------|--------|-----------------|
| Password hashing | ✅ SHA-256 | ⚠️ Upgrade to BCrypt |
| Token security | ✅ JWT + signatures | ✅ Production-ready |
| HTTPS | ❌ Missing | ✅ Required |
| Rate limiting | ❌ Missing | ✅ Prevent brute force |
| Refresh tokens | ❌ Missing | ✅ Better UX |

**Bottom line**: Great for learning, needs work for production.

---

## 🎓 Key Concepts

### Why Separate Layers?

**Bad** (everything in one file):
```java
@PostMapping("/login")
public ResponseEntity login() {
    String email = request.get("email");
    // Hash password here
    // Query database here  
    // Generate token here
    // All mixed together! 😵
}
```

**Good** (layered):
```java
@PostMapping("/login")              // Controller
public ResponseEntity login() {
    return authService.login(email); // Service handles logic
}
```

### Thread Safety = JPA/Hibernate Handles It!

```java
// Old way with HashMap:
Map<String, User> users = new HashMap<>();
// ❌ Two users register at same time = CRASH!

// Now with JPA:
@Repository
public interface UserRepository extends JpaRepository<User, Long> {}
// ✅ Database handles concurrency!
// ✅ Transactions = ACID compliance
// ✅ Connection pooling = Performance
```

---

## 🚀 Quick Start APIs

### Register
```bash
POST http://localhost:8080/api/auth/register
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "secret123",
  "phoneNumber": "+1234567890"
}

Response: 201 Created
{
  "token": "MToxNzM4Nzc5MjQ1MDAwOkt...",
  "email": "john@example.com"
}
```

### Login
```bash
POST http://localhost:8080/api/auth/login
{
  "email": "john@example.com",
  "password": "secret123"
}

Response: 200 OK
{
  "token": "MToxNzM4Nzc5MjQ1MDAwOkt...",
  "email": "john@example.com"
}
```

### Get User Info
```bash
GET http://localhost:8080/api/auth/me
Headers:
  Authorization: Bearer MToxNzM4Nzc5MjQ1MDAwOkt...

Response: 200 OK
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

### Admin: Get All Users (requires ROLE_ADMIN)
```bash
GET http://localhost:8080/api/admin/users
Headers:
  Authorization: Bearer <admin_token>

Response: 200 OK
[
  {
    "id": 1,
    "email": "admin@example.com",
    "firstName": "Admin",
    "roles": ["ROLE_USER", "ROLE_ADMIN"]
  }
]
```

### Admin: Add Role to User
```bash
POST http://localhost:8080/api/admin/add-role
Headers:
  Authorization: Bearer <admin_token>
{
  "email": "john@example.com",
  "role": "ROLE_MODERATOR"
}

Response: 200 OK
{
  "message": "Role added successfully"
}
```

### H2 Database Console
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/authdb
Username: sa
Password: (empty)
```

---

## 📁 File Structure

```
src/main/java/com/softwarearchi/archi/
├── ArchiApplication.java        ← Main entry point
├── controllers/
│   ├── AuthController.java      ← Auth HTTP endpoints
│   └── AdminController.java     ← Admin HTTP endpoints
├── services/
│   ├── AuthService.java         ← Authentication logic
│   └── UserService.java         ← User management
├── utils/
│   └── JwtUtil.java             ← JWT utility (token generation/validation)
├── repository/
│   ├── UserRepository.java      ← User JPA repository
│   └── RoleRepository.java      ← Role JPA repository
├── config/
│   ├── SecurityConfig.java      ← Spring Security config
│   └── DataInitializer.java     ← Creates default roles
└── models/
    ├── User.java                ← User entity
    └── Role.java                ← User roles

src/main/resources/
├── application.properties       ← H2 database config
└── static/                      ← Frontend (HTML/JS/CSS)

data/
└── authdb.mv.db                 ← H2 database file
```

---

## ✅ Remember

1. **Controller** = Handles HTTP, no logic
2. **Service** = Business logic, no HTTP
3. **Storage** = Data only, no logic
4. **Tokens** = JWT (stateless, secure, no DB storage)
5. **Security** = JWT-based, stateless, production-ready

**Next**: Check out MONITORING.md to see how logging works! 📊
