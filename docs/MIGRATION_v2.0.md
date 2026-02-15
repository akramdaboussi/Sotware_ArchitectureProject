## Migrating from In-Memory to Database (H2/JPA) & JWT

### Why migrate?
- Persistent storage (data survives restarts)
- Realistic production-like architecture
- Enables advanced features (relations, queries, migrations)

### Migration Steps

1. **Remove InMemoryStorage**
   - Delete `InMemoryStorage.java` and all usages in services/controllers.
2. **Add JPA Entities and Repositories**
   - Create `User`, `Role` entities with JPA annotations.
   - Create `UserRepository`, `RoleRepository` interfaces.
   - Remove `Token` entity and repository (JWT is now stateless and not stored in DB).
3. **Update Services**
   - Refactor `UserService`, `AuthService` to use repositories instead of in-memory maps.
   - Remove all code related to token storage and revocation.
4. **Configure H2 Database**
   - In `application.properties`:
     - `spring.datasource.url=jdbc:h2:file:./data/authdb`
     - `spring.datasource.driver-class-name=org.h2.Driver`
     - `spring.datasource.username=sa`
     - `spring.datasource.password=` (empty)
     - `spring.jpa.hibernate.ddl-auto=update`
     - `spring.jpa.show-sql=false` (optional, disables SQL logs)
5. **Migrate to JWT Authentication**
   - Implement JWT logic in `utils/JwtUtil.java`.
   - Update `AuthService` and `SecurityConfig` to use JWT for authentication and authorization.
   - Remove session and token storage logic from controllers/services.
6. **Test with H2 Console**
   - Start app, go to `/h2-console`, check tables and data.
7. **(Optional) Prepare for PostgreSQL/MySQL**
   - Add JDBC driver to `pom.xml`
   - Update datasource URL and credentials
   - Plan migration scripts (Flyway/Liquibase)

### Data Migration Tips
- If you have important data in memory, export it before migration.
- Write scripts to import/export users, roles, tokens if needed.
- Test all endpoints after migration.

### Troubleshooting
- **Table not found**: Check JPA entity annotations and `ddl-auto` setting.
- **Mapping errors**: Ensure all fields are mapped and relations are correct.
- **Connection errors**: Check JDBC URL, driver, and credentials.

### Rollback
- If migration fails, you can temporarily revert to the in-memory version (restore old code), but fix issues before retrying DB migration.

---

For more details, see [DATABASE.md] and [ARCHITECTURE.md].
