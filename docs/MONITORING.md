# 📊 Monitoring & Debugging Guide

> **TL;DR**: Every layer logs what it's doing. Read the logs = Understand what's happening.

---

## 🏷️ Log Tags (How to Read Logs)

| Tag | Layer | What It Means |
|-----|-------|---------------|
| `[CONTROLLER]` | HTTP Layer | Request received/sent |
| `[SERVICE-AUTH]` | Auth Logic | Register/login/logout |
| `[SERVICE-USER]` | User Logic | Password hashing/verification |
| `[SERVICE-TOKEN]` | Token Logic | Token create/validate |

### Log Level Guide
- **DEBUG** 🔍 = Detailed steps (password hashing, token checks)
- **INFO** ℹ️ = Normal operations (login success, user created)
- **WARN** ⚠️ = Problems (failed login, invalid token)
- **ERROR** 🚨 = Critical issues (system failures)

---

## 📝 Real Log Examples

### ✅ Successful Registration
```
INFO  [CONTROLLER] Registration request received
DEBUG [CONTROLLER] Registration data: email=john@example.com
INFO  [SERVICE-AUTH] 📝 Starting registration for email: john@example.com
DEBUG [SERVICE-AUTH] Checking if email exists
INFO  [SERVICE-USER] Creating user: john@example.com
DEBUG [SERVICE-USER] Hashing password (SHA-256)
INFO  [SERVICE-USER] ✅ User saved with ID: 1
INFO  [SERVICE-TOKEN] 🎫 Generating new token for user ID: 1
INFO  [SERVICE-TOKEN] ✅ Token generated, expires at: 2026-02-06T18:51:47
INFO  [CONTROLLER] ✅ Registration successful
```

### ✅ Successful Login
```
INFO  [CONTROLLER] Login request received
INFO  [SERVICE-AUTH] 🔐 Login attempt for email: john@example.com
DEBUG [SERVICE-AUTH] User found: ID=1
DEBUG [SERVICE-USER] Verifying password
DEBUG [SERVICE-USER] ✅ Password verification successful
INFO  [SERVICE-TOKEN] 🎫 Generating new token
INFO  [CONTROLLER] ✅ Login successful
```

### ❌ Failed Login (Wrong Password)
```
INFO  [CONTROLLER] Login request received
INFO  [SERVICE-AUTH] 🔐 Login attempt for email: john@example.com
DEBUG [SERVICE-USER] Verifying password
DEBUG [SERVICE-USER] ❌ Password verification failed
WARN  [SERVICE-AUTH] ❌ Login failed: Invalid password
WARN  [CONTROLLER] ❌ Login failed
```

### ❌ Failed Login (User Not Found)
```
INFO  [CONTROLLER] Login request received
INFO  [SERVICE-AUTH] 🔐 Login attempt for email: wrong@example.com
WARN  [SERVICE-AUTH] ❌ Login failed: User not found
WARN  [CONTROLLER] ❌ Login failed
```

---

## 🐛 Debugging with VS Code

### Step 1: Set Breakpoints

Click to the left of line numbers in these KEY locations:

**AuthController.java**:
- Line where you call `authService.login()`
- Line where you call `authService.register()`

**AuthService.java**:
- After `UserService.findByEmail()` returns
- Before `tokenService.generateToken()`

**UserService.java**:
- Inside `verifyPassword()` where it compares hashes

**TokenService.java**:
- Where token is created

### Step 2: Attach Debugger

1. Stop your current server (Ctrl+C)
2. Run with debug mode:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```
3. In VS Code: Run → Start Debugging (F5)

### Step 3: Trigger Request

Use the UI at `http://localhost:8080` or send API request.

### Step 4: Inspect Variables

When breakpoint hits, hover over variables:
- `email` - What email was sent?
- `password` - Raw password
- `user.getPassword()` - Hashed password
- `token` - Generated token
- `hashedInput` vs `hashedPassword` - Do they match?

---

## 🔍 Common Issues & How to Debug

### Issue 1: "Invalid email or password"

**What to check in logs:**
```
WARN [SERVICE-AUTH] ❌ Login failed: User not found
```
→ **Solution**: User doesn't exist. Register first!

```
DEBUG [SERVICE-USER] ❌ Password verification failed
```
→ **Solution**: Wrong password. Check what you're typing!

**Debug with breakpoint:**
```java
// In UserService.verifyPassword()
String hashedInput = hashPassword(rawPassword); // ← BREAKPOINT HERE
boolean matches = hashedInput.equals(hashedPassword);
// Inspect: Are hashedInput and hashedPassword the same?
```

### Issue 2: "Invalid or expired token"

**What to check in logs:**
```
WARN [SERVICE-TOKEN] ❌ Invalid or expired token
```

**Possible causes:**
- Token expired (>24 hours old)
- You logged out (token revoked)
- Server restarted (in-memory storage cleared)

**Debug with breakpoint:**
```java
// In TokenService.getUserIdFromToken()
Token token = storage.findTokenByValue(tokenValue); // ← BREAKPOINT HERE
if (token != null && token.isValid()) {
    // Check: token.isRevoked()? token.getExpiresAt()?
}
```

### Issue 3: "Email already exists"

**What to check in logs:**
```
WARN [SERVICE-AUTH] ❌ Registration failed: Email already exists
```

**Solutions:**
1. Use a different email
2. Restart server (clears in-memory storage)

---

## 🎯 Debug Workflow Example

**Scenario**: Login not working

1. **Check logs** → See `WARN [SERVICE-USER] ❌ Password verification failed`
2. **Set breakpoint** → `UserService.verifyPassword()`
3. **Send login request** → Breakpoint triggers
4. **Inspect variables**:
   - `rawPassword` = "password123"
   - `hashedPassword` = "XYZ..."
   - `hashedInput` = "ABC..." ← Different!
5. **Find problem** → Password doesn't match stored hash
6. **Solution** → Use correct password or re-register

---

## ⚙️ Change Log Levels

Edit `src/main/resources/application.properties`:

```properties
# Show everything (very verbose)
logging.level.com.softwarearchi.archi=DEBUG

# Show only important stuff
logging.level.com.softwarearchi.archi=INFO

# Show only warnings and errors
logging.level.com.softwarearchi.archi=WARN
```

---

## 🎨 Emoji Legend

| Emoji | Meaning |
|-------|---------|
| 📝 | Registration |
| 🔐 | Login |
| 🚪 | Logout |
| 🔍 | Fetching user |
| 🎫 | Token operation |
| ✅ | Success |
| ❌ | Failure |
| ⚠️ | Warning |

---

## 💡 Pro Tips

1. **Always check logs first** before debugging
2. **Look for emojis** to quickly see what's happening
3. **Set breakpoints** at decision points (if/else)
4. **Inspect variables** when password/token operations fail
5. **Use DEBUG level** when learning, INFO for normal use

**Next**: Try the hands-on exercises in EXERCISES.md! 🚀
