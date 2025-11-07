# Test Setup Guide

## Overview

This project uses **Testcontainers** for integration testing with a PostgreSQL database. Tests run in isolated Docker containers, eliminating the need for manual database setup.

## Recent Updates (November 2025)

1. **Cloud Database Integration**
   - Production now uses cloud-hosted PostgreSQL
   - Tests continue to use Testcontainers for isolation
   - No dependency on local database installation

2. **New Features Tested**
   - Auto-creation of Products and DutyTypes in tariff management
   - Enhanced TariffManagementController with full CRUD operations
   - Combined Duty calculations with mixed/compound modes
   - Improved error handling and validation

3. **Test Configuration Updates**
   - Removed individual test runs (DutyServiceTest, AuditLogServiceTest)
   - Consolidated to single test execution with proper profiling
   - Added JaCoCo code coverage reporting
   - Added test summary action for better GitHub reporting

4. **CI/CD Improvements**
   - Docker Buildx setup for Testcontainers
   - Maven dependency caching for faster builds
   - Coverage reports uploaded as artifacts
   - Test results retained for 30 days

## Prerequisites

### Local Development

- **Java 21** (Temurin distribution recommended)
- **Maven 3.8+**
- **Docker** (for Testcontainers)
  - Docker Desktop (Windows/Mac)
  - Docker Engine (Linux)

### GitHub Actions (Automated)

All prerequisites are automatically configured in the CI/CD pipeline.

## Running Tests Locally

### 1. Ensure Docker is Running

```bash
docker --version
```

Expected output: `Docker version 20.x.x` or higher

### 2. Run All Tests

```bash
mvn clean test
```

This will:
- Start a PostgreSQL container automatically
- Run all tests in the `test` profile
- Generate coverage reports in `target/site/jacoco/`
- Display test results in the terminal

### 3. Run Specific Test Classes

```bash
# Run TariffManagement tests only
mvn test -Dtest=TariffManagementControllerIntegrationTest

# Run all repository tests
mvn test -Dtest=*RepositoryTest

# Run specific test method
mvn test -Dtest=TariffManagementControllerIntegrationTest#testCreateTariffSuccess
```

### 4. View Coverage Report

After running tests:

```bash
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html  # Windows
```

## Test Configuration

### Test Profile (`application-test.properties`)

```properties
# Testcontainers automatically starts PostgreSQL
spring.datasource.url=jdbc:tc:postgresql:16-alpine:///testdb
spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver

# Schema auto-creation for tests
spring.jpa.hibernate.ddl-auto=create-drop

# Minimal logging for cleaner output
spring.jpa.show-sql=false
logging.level.root=WARN
```

### Environment Variables (for OAuth2 mocking)

```bash
export COGNITO_ISSUER=https://dummy-issuer.com
export COGNITO_TOKEN_SIGNING_KEY_URL=https://dummy-issuer.com/.well-known/jwks.json
export COGNITO_DOMAIN=dummy-domain
export AWS_REGION=us-east-1
export COGNITO_CLIENT_ID=dummy-client-id
export REDIS_URL=localhost
export REDIS_PORT=6379
export REDIS_USERNAME=""
export REDIS_PASSWORD=""
```

These are automatically set in GitHub Actions.

## Test Structure

### Integration Tests

- **TariffManagementControllerIntegrationTest** - Complete CRUD operations with validation
- **TariffScheduleRepositoryTest** - Database operations for tariff schedules
- **ProductRepositoryTest** - Product entity persistence
- **DutyTypeRepositoryTest** - Duty type repository tests
- **CountryRepositoryTest** - Country data management

### Service Tests

- **DutyServiceTest** - Tariff calculation logic (Ad Valorem, Specific, Combined)
- **AuditLogServiceTest** - Audit logging functionality
- **DataLoaderServiceTest** - CSV data import

### Repository Tests

- **AdValoremDutyRepositoryTest**
- **SpecificDutyRepositoryTest**
- **CombinedDutyRepositoryTest**
- **OtherDutyRepositoryTest**

## GitHub Actions Workflow

### Trigger Events

- Push to `main`, `develop`, or `backend` branches
- Pull requests to `main`

### Workflow Steps

1. **Checkout code**
2. **Set up JDK 21** with Maven caching
3. **Set up Docker** for Testcontainers
4. **Run all tests** with test profile
5. **Generate coverage report** (JaCoCo)
6. **Upload artifacts**:
   - Test results (Surefire reports)
   - Coverage reports (JaCoCo HTML)
7. **Test summary** - Visual report in PR checks

### Viewing Results

1. Go to **Actions** tab in GitHub
2. Click on the latest workflow run
3. View **Test Summary** for quick overview
4. Download **test-results** artifact for detailed reports
5. Download **coverage-report** artifact to view coverage HTML

## Troubleshooting

### Docker Not Available

**Error:** `Could not find a valid Docker environment`

**Solution:**
```bash
# Check Docker is running
docker ps

# Start Docker Desktop (Windows/Mac)
# Or start Docker service (Linux)
sudo systemctl start docker
```

### Testcontainers Timeout

**Error:** `Container startup failed`

**Solution:**
```bash
# Pull PostgreSQL image manually
docker pull postgres:16-alpine

# Increase timeout in test (if needed)
# Add to test class:
@Testcontainers(disabledWithoutDocker = true)
```

### Port Already in Use

**Error:** `Port 5432 is already allocated`

**Solution:**
```bash
# Stop local PostgreSQL if running
sudo systemctl stop postgresql  # Linux
brew services stop postgresql@14  # macOS

# Or let Testcontainers use random ports (default behavior)
```

### Maven Build Failure

**Error:** `Tests compilation failed`

**Solution:**
```bash
# Clean and rebuild
mvn clean compile

# Update dependencies
mvn clean install -U

# Skip tests temporarily to check compilation
mvn clean install -DskipTests
```

## Coverage Goals

### Current Targets

- **Line Coverage:** 50% minimum (enforced by JaCoCo)
- **Branch Coverage:** 40% recommended
- **Package Coverage:** 50% minimum

### Excluded from Coverage

- Configuration classes
- DTOs (Data Transfer Objects)
- Entity classes (simple getters/setters)
- Main application class

## Best Practices

### 1. Write Isolated Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class MyIntegrationTest {
    // Test implementation
}
```

### 2. Use Mock Users for Security

```java
@Test
@WithMockUser(authorities = "Admin")
void testAdminEndpoint() {
    // Test admin-only features
}
```

### 3. Test Edge Cases

```java
@Nested
@DisplayName("Validation Tests")
class ValidationTests {
    @Test
    void testNegativeRate() { /* ... */ }
    
    @Test
    void testExceedsMaximum() { /* ... */ }
}
```

### 4. Clean Test Data

```java
@AfterEach
void cleanup() {
    // Transactional rollback handles this automatically
    // Manual cleanup only if needed
}
```

## New Test Features (2025 Update)

### Auto-Creation Testing

Tests verify that admins can create tariffs with:
- New HS codes (auto-creates Products)
- New duty types (auto-creates DutyTypes)

```java
@Test
void testAutoCreateProduct() {
    // Verify product creation with new HS code
}
```

### Combined Duty Testing

Tests verify both Mixed (max) and Compound (sum) calculations:

```java
@Test
void testCombinedDutyMixed() {
    // Test: max(adValorem, specific)
}

@Test
void testCombinedDutyCompound() {
    // Test: adValorem + specific
}
```

## Additional Resources

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)

## Support

For issues or questions:
1. Check existing test failures in GitHub Actions
2. Review test logs in `target/surefire-reports/`
3. Consult team documentation
4. Create an issue with test failure details
