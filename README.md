# Compliance Obligation Register

A comprehensive compliance management system built with Spring Boot, React, and AI-powered analytics.

## Architecture

- **Backend**: Spring Boot with PostgreSQL and Redis
- **Frontend**: React application served via Nginx
- **AI Service**: Python Flask service for compliance analytics
- **Database**: PostgreSQL for data persistence
- **Cache**: Redis for performance optimization

## Quick Start with Docker Compose

### Prerequisites

- Docker and Docker Compose
- At least 4GB RAM available for containers
- Ports 80, 8080, 5000, 5432, 6379 available

### 1. Environment Configuration

Copy the example environment file and configure your settings:

```bash
cp .env.example .env
```

Edit `.env` with your specific configuration (see .env.example for all options).

### 2. Build and Run All Services

```bash
docker-compose up --build
```

This will start:
- PostgreSQL database on port 5432
- Redis cache on port 6379
- Spring Boot backend on port 8080
- Python AI service on port 5000
- React frontend on port 80

### 3. Access the Application

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **AI Service**: http://localhost:5000

## Traditional Development Setup

### 1. Configuration
1. Copy `backend/src/main/resources/application.properties.template` to `application.properties`
2. Fill in your actual configuration values:
   - **Email Settings**: Replace with your Gmail credentials
     - Generate App Password from [Google Account Settings](https://myaccount.google.com/apppasswords)
   - **Database Settings**: Configure your database connection
   - **Security Settings**: Set secure JWT secret and other security configurations

### 2. Email Configuration
For Gmail SMTP:
1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password
3. Use your Gmail address as username and the App Password as password

### 3. Database Setup
The application uses Flyway for database migrations. Supported databases:
- H2 (for development/testing)
- PostgreSQL (for production)

### 4. Running the Application
```bash
cd backend
mvn spring-boot:run
```

## Docker Compose Commands

### Development

```bash
# Start all services
docker-compose up --build

# Start in background
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (reset database)
docker-compose down -v
```

### Individual Services

```bash
# Run only backend
docker-compose up backend

# Run only database
docker-compose up postgres redis

# Scale services
docker-compose up --scale backend=3
```

### Database Access

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U compliance_user -d compliance_db

# Access Redis CLI
docker-compose exec redis redis-cli
```

## Features

- User authentication and authorization (JWT)
- Compliance obligation management (CRUD)
- Email notifications for new assignments
- Scheduled alerts for overdue and upcoming obligations
- Weekly summary reports
- Audit logging with Spring AOP
- Pagination and CSV export
- AI-powered compliance analysis
- Role-based access control (RBAC)

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Compliance Obligations
- `GET /api/obligations/all` - Get all obligations (paginated)
- `GET /api/obligations/{id}` - Get obligation by ID
- `POST /api/obligations` - Create new obligation
- `PUT /api/obligations/{id}` - Update obligation
- `DELETE /api/obligations/{id}` - Delete obligation
- `GET /api/obligations/export` - Export to CSV

### AI Service
- `POST /ai/analyze` - AI compliance analysis
- `POST /ai/predict` - Deadline prediction

## Health Checks

- **Backend**: http://localhost:8080/actuator/health
- **AI Service**: http://localhost:5000/health

## Security Note

- Never commit `.env` or `application.properties` to version control
- Use environment variables or external configuration for sensitive data in production
- The `.gitignore` file excludes sensitive configuration files

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 80, 8080, 5000, 5432, 6379 are available
2. **Memory issues**: Increase Docker memory allocation to at least 4GB
3. **Database connection**: Check .env file configuration
4. **Build failures**: Ensure Docker has sufficient disk space

### Reset Everything

```bash
# Stop and remove all containers and volumes
docker-compose down -v

# Clean rebuild
docker-compose up --build --force-recreate
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.