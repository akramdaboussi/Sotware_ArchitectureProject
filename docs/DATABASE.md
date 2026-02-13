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

#### tokens
| Column       | Type           | Description                |
|-------------|----------------|----------------------------|
| id          | Long (PK)      | Token ID (auto-generated)  |
| token       | String (unique)| Base64-encoded token value |
| token_type  | String         | Token type (ACCESS)        |
| revoked     | Boolean        | Token revoked (logout)     |
| expired     | Boolean        | Token expired              |
| created_at  | LocalDateTime  | Creation timestamp         |
| expires_at  | LocalDateTime  | Expiry timestamp           |
| user_id     | Long (FK)      | Owner (User) ID            |

### Relationships
- **User <-> Role**: Many-to-Many (user_roles join table)
- **User <-> Token**: One-to-Many (user_id foreign key in tokens)

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
-- List all tokens
SELECT * FROM tokens;
-- Show user-role assignments
SELECT * FROM user_roles;
```
