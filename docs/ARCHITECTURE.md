# 🏗️ Architecture Quick Guide

> **TL;DR**: A custom authentication system built with pure Java - no JWT libraries, no database (yet!). Learn by doing.

---

## 🎯 What Is This?

A **3-layer authentication API** that handles user registration, login, and token management:

```
Browser → Controller → Service → Storage
  ↓          ↓           ↓          ↓
 UI      HTTP/JSON   Business   In-Memory
                      Logic       Data
```

---

## 🧱 The 3 Layers Explained

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
    String token = authService.login(email, password); // ← Call service
    return ResponseEntity.ok(Map.of("token", token));
}
```

### Layer 2: **Service** (Business Brain)
**Job**: Make decisions  
**Files**: `AuthService`, `UserService`, `TokenService`  
**Does**: Validate, hash passwords, generate tokens  
**Never Does**: HTTP stuff, know about JSON

```java
// Service = The brain
public String login(String email, String password) {
    User user = userService.findByEmail(email);      // Find
    if (!verifyPassword(password, user.getPassword())) {  // Check
        throw new RuntimeException("Invalid credentials");
    }
    return tokenService.generateToken(user);         // Create token
}
```

### Layer 3: **Storage** (Data Keeper)
**Job**: Store and find stuff  
**Files**: `InMemoryStorage.java`, models (`User`, `Token`, `Role`)  
**Does**: Save/retrieve data, thread safety  
**Never Does**: Validation, business rules

```java
// Storage = The vault
private final Map<String, User> users = new ConcurrentHashMap<>();

public void saveUser(User user) {
    user.setId(++userIdCounter);
    users.put(user.getEmail(), user);
}
```

---

## 🔑 How Authentication Works

### Registration: 5 Steps
```mermaid
graph LR
    A[1. POST<br/>/register] --> B[2. Hash<br/>password]
    B --> C[3. Save<br/>user]
    C --> D[4. Create<br/>token]
    D --> E[5. Return<br/>201 + token]
    
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
    C --> D[4. Generate<br/>token]
    
    style A fill:#818cf8
    style B fill:#10b981
    style C fill:#f59e0b
    style D fill:#ec4899
```

### Token: Our Custom Format
```
Format: userId:timestamp:randomString
Example: 1:1738779245000:Kx9mP2nQ7rT8vW1yZ3aB4cD5eF6gH7

Encoded (Base64):
MToxNzM4Nzc5MjQ1MDAwOkt4OW1QMm5RN3JUOHZXMXlaM2FCNGNENWVGNmdINw==
```

**Why custom?**
- ✅ No external dependencies (pure Java)
- ✅ Learn how tokens actually work
- ⚠️ Not production-secure (use JWT in real apps)

---

## 🤔 Why These Design Choices?

| Choice | Why? | Trade-off |
|--------|------|-----------|
| **Custom tokens** (not JWT) | Educational - see how it works | Less secure |
| **SHA-256** (not BCrypt) | Built into Java | Less secure |
| **In-memory** (not database) | Zero configuration | Lost on restart |
| **Layered architecture** | Clean, testable, maintainable | More files |

---

## 📊 The Complete Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant AuthService
    participant UserService
    participant TokenService
    participant Storage
    
    User->>Controller: POST /login
    Controller->>AuthService: login(email, pwd)
    AuthService->>UserService: findByEmail()
    UserService->>Storage: Get user
    Storage-->>UserService: User
    AuthService->>UserService: verifyPassword()
    UserService-->>AuthService: ✅ Valid
    AuthService->>TokenService: generateToken()
    TokenService->>Storage: Save token
    TokenService-->>AuthService: token
    AuthService-->>Controller: token
    Controller-->>User: 200 OK {token}
```

---

## 🔒 Security Reality Check

| Feature | Status | Production Need |
|---------|--------|-----------------|
| Password hashing | ✅ SHA-256 | ⚠️ Upgrade to BCrypt |
| Token security | ✅ Base64 | ⚠️ Use JWT + signatures |
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

### Thread Safety = Why ConcurrentHashMap?

```java
// Regular HashMap:
Map<String, User> users = new HashMap<>();
// ❌ Two users register at same time = CRASH!

// ConcurrentHashMap:
Map<String, User> users = new ConcurrentHashMap<>();
// ✅ Thread-safe = Multiple requests = No problem!
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

---

## 📁 File Structure

```
src/main/java/com/softwarearchi/archi/
├── controllers/
│   └── AuthController.java      ← HTTP endpoints
├── services/
│   ├── AuthService.java         ← Authentication logic
│   ├── UserService.java         ← User management
│   └── TokenService.java        ← Token operations
├── storage/
│   └── InMemoryStorage.java     ← Data storage
└── models/
    ├── User.java                ← User entity
    ├── Token.java               ← Token entity
    └── Role.java                ← User roles
```

---

## ✅ Remember

1. **Controller** = Handles HTTP, no logic
2. **Service** = Business logic, no HTTP
3. **Storage** = Data only, no logic
4. **Tokens** = Our custom format (learn first, use JWT later)
5. **Security** = Educational level (upgrade for production)

**Next**: Check out MONITORING.md to see how logging works! 📊
