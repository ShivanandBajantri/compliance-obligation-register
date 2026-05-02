# Compliance Obligation Register

A Spring Boot application for managing compliance obligations with email notifications and scheduled alerts.

## Setup Instructions

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

## Features
- User authentication and authorization
- Compliance obligation management
- Email notifications for new assignments
- Scheduled alerts for overdue and upcoming obligations
- Weekly summary reports

## Security Note
- Never commit `application.properties` to version control
- Use environment variables or external configuration for sensitive data in production
- The `.gitignore` file excludes sensitive configuration files