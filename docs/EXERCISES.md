# 🎯 Quick Start Exercises

> **Goal**: Master authentication fundamentals in 20 minutes. 8 focused activities.

---

## 🏃 Express Track (20 minutes total)

### Exercise 1: **API Endpoint Hunt** ⏱️ 2 min
**Find all 4 REST endpoints and test one**

Open `AuthController.java` and list:
- ☐ Registration endpoint
- ☐ Login endpoint
- ☐ Logout endpoint  
- ☐ Get user info endpoint

**Quick Test**: Visit http://localhost:8080 and register a new user!

---

### Exercise 2: **Follow the Password** ⏱️ 3 min
**Trace how a password becomes a hash**

1. Open `AuthController.register()` → receives `password`
2. Follow to `AuthService.register()` → passes it along
3. Find `UserService.createUser()` → calls `hashPassword()`
4. Look at `hashPassword()` method → **SHA-256** magic happens!

**Question**: Is the password ever stored in plain text? ___________  
**Answer**: No! Always hashed.

---

### Exercise 3: **Decode a Token** ⏱️ 2 min
**See what's inside the authentication token**

1. Open `TokenService.generateToken()`
2. Find the line: `String payload = user.getId() + ":" + ...`
3. Note the format: `userId:timestamp:randomString`

**Quick Check**: What's inside the token?
- [ ] Password ❌
- [ ] User ID ✅
- [ ] Email ❌

---

### Exercise 4: **Watch Registration Live** ⏱️ 3 min
**Observe real-time logs**

1. Look at your terminal running the server
2. Register a new user from the UI
3. Watch the logs appear with tags:
   - `[CONTROLLER]` → Request received
   - `[SERVICE-AUTH]` → Registration starts
   - `[SERVICE-USER]` → Password hashed
   - `[SERVICE-TOKEN]` → Token generated

**Count**: How many log lines appeared? (Approx) ___________

---

### Exercise 5: **Trigger an Error** ⏱️ 2 min
**See error handling in action**

Try logging in with:
- Email: `wrong@example.com`
- Password: `anything`

**Watch for**:
- Log: `WARN [SERVICE-AUTH] ❌ Login failed: User not found`
- Browser: Error message appears

**Question**: What HTTP status code was returned? ___________  
**Answer**: 401 Unauthorized

---

### Exercise 6: **Debug with Breakpoint** ⏱️ 4 min
**Step through authentication code**

1. Set breakpoint in `AuthService.login()` (click left of line number)
2. Login from the UI
3. When breakpoint hits, inspect variables:
   - `email` → What was entered
   - `user` → Found user object
   - `password` → Raw password
4. Press F10 to step through each line

**Cool Discovery**: Watch `tokenService.generateToken()` create the token!

---

### Exercise 7: **Add Your Own Log** ⏱️ 3 min
**Customize the system**

In `AuthService.register()`, after line `userService.createUser(user)`, add:

```java
logger.info("[SERVICE-AUTH] 🎉 Welcome aboard, {}!", firstName);
```

Save → Wait for auto-reload → Register someone new → See your log!

---

### Exercise 8: **Create Welcome Message** ⏱️ 3 min
**Enhance the API response**

In `AuthController.login()`, find where response is created and add:

```java
response.put("welcome", "Welcome back!");
```

**Test**: Login and check browser DevTools → Network → Response should include `"welcome": "Welcome back!"`

---

## ✅ Quick Checklist

- [ ] Exercise 1: Found all endpoints (2 min)
- [ ] Exercise 2: Traced password hashing (3 min)
- [ ] Exercise 3: Decoded token format (2 min)
- [ ] Exercise 4: Watched live logs (3 min)
- [ ] Exercise 5: Triggered error (2 min)
- [ ] Exercise 6: Used debugger (4 min)
- [ ] Exercise 7: Added custom log (3 min)
- [ ] Exercise 8: Enhanced response (3 min)

**Total**: ~20 minutes

---

## 🎓 What You Learned

✅ **Architecture**: Controllers → Services → Storage  
✅ **Security**: Password hashing (SHA-256)  
✅ **Tokens**: Custom Base64 format  
✅ **Logging**: How to read layered logs  
✅ **Debugging**: VS Code breakpoints  
✅ **Coding**: How to customize the system

---

## 🚀 Want More?

**Next Challenges** (optional, 10-15 min each):

1. **Change Token Expiry**: Make tokens expire in 1 hour instead of 24
2. **Add Password Strength**: Reject passwords shorter than 8 characters
3. **Create User Count Endpoint**: `GET /api/auth/count` returns total users
4. **Add Error Codes**: Distinguish "user not found" from "wrong password"

**Big Projects** (when you have more time):
- Implement email verification
- Add password reset flow
- Migrate to JWT tokens
- Add database with JPA
- Implement role-based permissions

---

## 💡 Pro Tips

- **Read logs first** before debugging
- **Set breakpoints** at decision points (if/else)
- **Use DEBUG log level** when learning
- **Check ARCHITECTURE.md** for layer explanations
- **Check MONITORING.md** for debugging scenarios

**You're ready to build authentication systems!** 🎉
