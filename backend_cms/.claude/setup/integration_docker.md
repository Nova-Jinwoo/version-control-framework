# Docker Integration Plan for Spring Boot Application

## Overview
This document outlines the steps to containerize the Spring Boot backend_cms application using Docker and Docker Compose for local development.

## Implementation Steps

### Step 1: Create Dockerfile.local
**File:** `backend_cms/Dockerfile.local`

Purpose: Multi-stage build for local development with hot reload support

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Alternative for development with live reload:
```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src src
EXPOSE 8080
CMD ["./mvnw", "spring-boot:run"]
```

### Step 2: Create docker-compose.yml
**File:** `backend_cms/docker-compose.yml`

Purpose: Orchestrate Spring Boot app with MongoDB

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:7.0
    container_name: vcf_mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
      - mongodb_config:/data/configdb
    environment:
      MONGO_INITDB_DATABASE: vcf_dev
    networks:
      - vcf_network

  backend-cms:
    build:
      context: .
      dockerfile: Dockerfile.local
    container_name: vcf_backend_cms
    restart: unless-stopped
    ports:
      - "8080:8080"
    depends_on:
      - mongodb
    environment:
      SPRING_APPLICATION_NAME: version-control-framework
      SERVER_PORT: 8080
      MONGODB_URI: mongodb://mongodb:27017/
      MONGODB_CMS_DATABASE: vcf_dev
    networks:
      - vcf_network
    volumes:
      # For hot reload during development (optional)
      - ./src:/app/src
      - ./pom.xml:/app/pom.xml
      - maven_cache:/root/.m2

volumes:
  mongodb_data:
  mongodb_config:
  maven_cache:

networks:
  vcf_network:
    driver: bridge
```

### Step 3: Create .dockerignore
**File:** `backend_cms/.dockerignore`

Purpose: Exclude unnecessary files from Docker context

```
target/
*.class
*.jar
*.war
*.ear
*.logs
*.log
.git
.gitignore
.mvn/wrapper/maven-wrapper.jar
.idea
*.iws
*.iml
*.ipr
.settings
.classpath
.project
.vscode
*.swp
*.swo
.DS_Store
```

### Step 4: Create Environment File (Optional)
**File:** `backend_cms/.env.docker`

Purpose: Centralize environment variables for docker-compose

```env
# Application
SPRING_APPLICATION_NAME=version-control-framework
SERVER_PORT=8080

# MongoDB
MONGODB_URI=mongodb://mongodb:27017/
MONGODB_CMS_DATABASE=vcf_dev
```

Then modify docker-compose.yml to use env_file:
```yaml
backend-cms:
  env_file:
    - .env.docker
```

### Step 5: Add Docker Scripts to package.json (Optional)
**File:** Create `backend_cms/scripts/docker.sh`

Purpose: Convenience scripts for Docker operations

```bash
#!/bin/bash

case "$1" in
  "build")
    docker-compose build
    ;;
  "up")
    docker-compose up -d
    ;;
  "down")
    docker-compose down
    ;;
  "logs")
    docker-compose logs -f backend-cms
    ;;
  "restart")
    docker-compose restart backend-cms
    ;;
  "clean")
    docker-compose down -v
    ;;
  *)
    echo "Usage: ./scripts/docker.sh {build|up|down|logs|restart|clean}"
    exit 1
    ;;
esac
```

### Step 6: MongoDB Initialization (Optional)
**File:** `backend_cms/docker/mongo-init/init.js`

Purpose: Initialize MongoDB with default data or indexes

```javascript
db = db.getSiblingDB('vcf_dev');

// Create collections
db.createCollection('contents');
db.createCollection('divisions');

// Create indexes
db.contents.createIndex({ "createdAt": 1 });
db.divisions.createIndex({ "name": 1 });

// Insert sample data (optional)
db.contents.insertOne({
  title: "Sample Content",
  description: "Docker initialization successful",
  createdAt: new Date()
});
```

Add to docker-compose.yml:
```yaml
mongodb:
  volumes:
    - ./docker/mongo-init:/docker-entrypoint-initdb.d
```

## Usage Instructions

### Build and Run
```bash
# Build the Docker image
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean start)
docker-compose down -v
```

### Development Workflow
1. Make code changes locally
2. If using hot reload, changes apply automatically
3. Otherwise, rebuild: `docker-compose build backend-cms`
4. Restart service: `docker-compose restart backend-cms`

### Testing the Setup
```bash
# Check if services are running
docker-compose ps

# Test the ping endpoint
curl http://localhost:8080/ping

# Access MongoDB
docker exec -it vcf_mongodb mongosh vcf_dev

# View Spring Boot logs
docker logs vcf_backend_cms
```

## File Structure After Implementation
```
backend_cms/
├── .dockerignore
├── Dockerfile.local
├── docker-compose.yml
├── .env.docker (optional)
├── docker/
│   └── mongo-init/
│       └── init.js (optional)
├── scripts/
│   └── docker.sh (optional)
└── [existing files...]
```

## Benefits of This Setup
1. **Consistency**: Same environment for all developers
2. **Isolation**: No need to install MongoDB locally
3. **Easy cleanup**: Simple teardown with docker-compose down
4. **Port management**: All services configured with standard ports
5. **Data persistence**: MongoDB data persisted in Docker volumes
6. **Network isolation**: Services communicate on internal Docker network

## Potential Improvements
1. Add health checks to docker-compose services
2. Use Docker secrets for sensitive data
3. Add nginx reverse proxy for production-like setup
4. Configure MongoDB replica set for production similarity
5. Add monitoring services (Prometheus, Grafana)

## Troubleshooting

### Common Issues and Solutions

1. **Port already in use**
   - Change port mapping in docker-compose.yml
   - Or stop conflicting service: `lsof -i :8080` / `lsof -i :27017`

2. **MongoDB connection refused**
   - Ensure MongoDB service is running: `docker-compose ps`
   - Check network connectivity: `docker network ls`
   - Verify environment variables

3. **Maven dependencies not downloading**
   - Clear Maven cache: `docker volume rm backend_cms_maven_cache`
   - Rebuild without cache: `docker-compose build --no-cache`

4. **Application not reflecting changes**
   - Ensure volumes are mounted correctly for hot reload
   - Rebuild image if changing dependencies

## Security Considerations
1. Don't commit .env.docker with sensitive data
2. Use specific image versions instead of 'latest'
3. Run containers as non-root user in production
4. Limit resource usage with deploy constraints