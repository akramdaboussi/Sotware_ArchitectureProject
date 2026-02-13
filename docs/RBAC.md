# RBAC.md — Role-Based Access Control Guide

## Overview

This document explains the Role-Based Access Control (RBAC) system implemented in this authentication project. It details the available roles, their permissions, how roles are assigned, and how RBAC is enforced in the codebase.

---

## What is RBAC?

RBAC (Role-Based Access Control) is a security paradigm that restricts system access to authorized users based on their assigned roles. Each role has specific permissions, and users can have one or more roles.

---

## Roles Defined

| Role            | Description                        | Permissions Summary                |
|-----------------|------------------------------------|------------------------------------|
| ROLE_USER       | Standard user                      | Access own profile, basic actions  |
| ROLE_ADMIN      | Administrator                      | Manage users, assign roles, admin endpoints |
| ROLE_MODERATOR  | Moderator                          | Moderate content, limited admin    |

---

## Permissions Matrix

| Endpoint                  | ROLE_USER | ROLE_ADMIN | ROLE_MODERATOR |
|---------------------------|:---------:|:----------:|:--------------:|
| /api/auth/register        |    ✅     |     ✅     |      ✅       |
| /api/auth/login           |    ✅     |     ✅     |      ✅       |
| /api/auth/logout          |    ✅     |     ✅     |      ✅       |
| /api/auth/me              |    ✅     |     ✅     |      ✅       |
| /api/admin/users          |           |     ✅     |      ✅       |
| /api/admin/add-role       |           |     ✅     |                |
| /api/admin/remove-role    |           |     ✅     |                |

- ✅ = Access allowed
- Blank = Access denied

---

## How Roles Are Assigned

- **Default:** New users are assigned `ROLE_USER` by default.
- **Admin Assignment:** Only users with `ROLE_ADMIN` can assign or remove roles using the `/api/admin/add-role` and `/api/admin/remove-role` endpoints.
- **Moderator:** `ROLE_MODERATOR` can be assigned by an admin for content moderation tasks.

---

## Enforcing RBAC in Code

- **Controllers:**
  - Admin endpoints are annotated to require `ROLE_ADMIN`.
  - Moderator endpoints (if any) require `ROLE_MODERATOR` or `ROLE_ADMIN`.
- **Service Layer:**
  - Business logic checks user roles before performing sensitive actions.
- **SecurityConfig:**
  - Configures endpoint access rules using Spring Security.

---

## Example: Restricting Access in Controller

```java
// Only admins can access this endpoint
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/admin/users")
public List<User> getAllUsers() {
    // ...
}
```

---

## How to Test RBAC

1. Register a new user (gets `ROLE_USER`)
2. Log in as an admin
3. Assign `ROLE_MODERATOR` to a user
4. Attempt to access admin endpoints as different roles
5. Verify access is correctly restricted

---

## Troubleshooting

- If a user cannot access an endpoint:
  - Check their assigned roles in the database
  - Ensure the endpoint is correctly annotated in the controller
  - Review `SecurityConfig` for access rules

---

## Extending RBAC

- Add new roles by updating the `Role` model and database
- Update `SecurityConfig` to define new access rules
- Document new roles and permissions in this file

---

## References
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-requests.html)
- [Project ARCHITECTURE.md](ARCHITECTURE.md)

---

*Last updated: February 2026*