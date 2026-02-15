## Database Schema and Setup

### Tables and Columns

#### users
| Column       | Type           | Description                |
|-------------|----------------|----------------------------|
| id          | Long (PK)      | User ID (auto-generated)   |
| first_name  | String         | User's first name          |
| last_name   | String         | User's last name           |
| phone_number| String         | User's phone number        |
| email       | String (unique)| User's email (login)       |
| password    | String         | Hashed password (SHA-256)  |
| enabled     | Boolean        | Account enabled/disabled   |
| created_at  | LocalDateTime  | Creation timestamp         |
| updated_at  | LocalDateTime  | Last update timestamp      |

#### roles
| Column       | Type           | Description                |
|-------------|----------------|----------------------------|
| id          | Long (PK)      | Role ID (auto-generated)   |
| name        | String (unique)| Role name (e.g. ROLE_USER) |
| description | String         | Role description           |


> **Note:** Since the migration to JWT, tokens are no longer stored in the database. JWTs are stateless and only exist on the client side.

### Relationships
- **User <-> Role**: Many-to-Many (user_roles join table)

### Setup Instructions

#### H2 (default)
- Configured in `src/main/resources/application.properties`
- Database file: `./data/authdb.mv.db`
- Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- JDBC URL: `jdbc:h2:file:./data/authdb`
- Username: `sa` (no password)

#### PostgreSQL/MySQL (planned)
- Update `spring.datasource.url`, `username`, `password` in `application.properties`
- Add JDBC driver dependency in `pom.xml`
- Run migration scripts (see MIGRATION_v2.0.md)

### Example Queries
```sql
-- List all users
SELECT * FROM users;
-- List all roles
SELECT * FROM roles;
-- (No more tokens table: JWTs are not stored in DB)
-- List all tokens
SELECT * FROM tokens;
-- Show user-role assignments
SELECT * FROM user_roles;
```
