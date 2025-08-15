# MongoDB Integration Plan for Spring Boot Application

## Overview
This document outlines the steps to integrate MongoDB with the Spring Boot backend_cms application, connecting to a local MongoDB instance with database name `vcf_dev`.

## Prerequisites
- Local MongoDB instance running (default port: 27017)
- Database: `vcf_dev`
- Spring Boot 3.5.4 application

## Implementation Steps

### Step 1: Add MongoDB Dependencies
**File:** `pom.xml`
- Add `spring-boot-starter-data-mongodb` dependency to enable MongoDB support
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### Step 2: Configure MongoDB Connection
**File:** `src/main/resources/application.properties`
- Add MongoDB connection properties:
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/vcf_dev
spring.data.mongodb.database=vcf_dev

# Optional: Additional MongoDB settings
spring.data.mongodb.auto-index-creation=true
```

### Step 3: Update Existing Entity Classes
Convert existing entity classes to MongoDB documents:

#### 3.1 Content Module
**Files to update:**
- `Content.java` - Add `@Document` annotation, replace `@Entity`
- `Exterior.java` - Add `@Document` annotation
- `Interior.java` - Add `@Document` annotation

Changes:
- Replace JPA annotations (`@Entity`, `@Table`) with MongoDB annotations (`@Document`)
- Replace `@Id` with MongoDB's `@Id` (org.springframework.data.annotation.Id)
- Remove JPA-specific annotations like `@GeneratedValue`

#### 3.2 Division Module
**File to update:**
- `Division.java` - Add `@Document` annotation

### Step 4: Update Repository Interfaces
Convert JPA repositories to MongoDB repositories:

**Files to update:**
- `ContentRepository.java` - Extend `MongoRepository<Content, String>`
- `ExteriorRepository.java` - Extend `MongoRepository<Exterior, String>`
- `InteriorRepository.java` - Extend `MongoRepository<Interior, String>`
- `DivisionRepository.java` - Extend `MongoRepository<Division, String>`

Changes:
- Replace `JpaRepository` with `MongoRepository`
- Update ID type from `Long` to `String` (MongoDB uses String IDs by default)

### Step 5: Create MongoDB Configuration (Optional)
**New File:** `src/main/java/com/example/backend_cms/config/MongoConfig.java`

Optional configuration class for custom MongoDB settings:
```java
@Configuration
@EnableMongoRepositories(basePackages = "com.example.backend_cms.modules")
public class MongoConfig {
    // Custom MongoDB configurations if needed
    // e.g., custom converters, auditing, etc.
}
```

### Step 6: Update Service Classes
Minimal changes needed:
- Update ID types from `Long` to `String` in service method signatures
- No major logic changes required as Spring Data abstracts the database operations

### Step 7: Update DTOs
**Files to update:**
- `ContentDto.java`
- `ExteriorDto.java`
- `InteriorDto.java`
- `DivisionDto.java`

Changes:
- Update ID field type from `Long` to `String`

### Step 8: Enable MongoDB Auditing (Optional)
If you need automatic timestamps:
1. Add `@EnableMongoAuditing` to main application class or config
2. Add `@CreatedDate` and `@LastModifiedDate` annotations to entities

### Step 9: Testing the Connection
1. Start MongoDB locally
2. Run the Spring Boot application
3. Check logs for successful MongoDB connection
4. Test endpoints through HealthController

## File Impact Summary

### Files to Modify:
1. `pom.xml` - Add dependency
2. `application.properties` - Add connection settings
3. Entity classes (4 files) - Update annotations
4. Repository interfaces (4 files) - Change parent interface
5. DTO classes (4 files) - Update ID types
6. Service classes (4 files) - Update method signatures for ID types

### New Files to Create:
1. `MongoConfig.java` (optional) - Custom MongoDB configuration

## Validation Steps
1. Verify MongoDB connection on application startup
2. Test CRUD operations through existing endpoints
3. Check MongoDB collections are created with correct names
4. Validate data persistence and retrieval

## Rollback Plan
If issues arise:
1. Revert pom.xml changes
2. Restore original entity annotations
3. Revert repository interfaces to JPA
4. Remove MongoDB configuration from application.properties

## Notes
- MongoDB uses String IDs by default (ObjectId)
- Collection names will be derived from class names (lowercase)
- No SQL migration scripts needed
- Indexes can be created via annotations or MongoDB directly