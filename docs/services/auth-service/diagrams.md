# Auth Service Diagrams

## ER Diagram
```mermaid
erDiagram
    users ||--o{ auth_otps : "receives OTP challenges"

    users {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR username "UNIQUE"
        VARCHAR email "UNIQUE"
        VARCHAR password_hash
        VARCHAR role "MEMBER | PLATFORM_ADMIN"
        VARCHAR provider "LOCAL | GOOGLE"
        BOOLEAN active
        BOOLEAN suspended
        DATETIME created_at
        DATETIME updated_at
    }

    auth_otps {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR email
        VARCHAR otp_hash
        VARCHAR purpose "REGISTER | LOGIN | RESET_PASSWORD"
        INT attempt_count
        DATETIME expires_at
        BOOLEAN used
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class AuthController {
        +register(RegisterRequest) AuthResponse
        +requestRegistrationOtp(RegisterRequest) OtpChallengeResponse
        +verifyRegistrationOtp(VerifyOtpRequest) AuthResponse
        +login(LoginRequest) AuthResponse
        +requestLoginOtp(LoginRequest) OtpChallengeResponse
        +verifyLoginOtp(VerifyOtpRequest) AuthResponse
        +refresh(String) AuthResponse
        +profile(Authentication) UserResponse
        +updateProfile(UpdateProfileRequest) UserResponse
        +resetPassword(ResetPasswordRequest) void
    }

    class AdminController {
        +getUsers() List~UserResponse~
        +changeRole(Long, String) UserResponse
        +suspendUser(Long) void
        +restoreUser(Long) void
        +deleteUser(Long) void
        +stats() Map
    }

    class InternalUserController {
        +getUser(Long) UserResponse
        +getByUsername(String) UserResponse
    }

    class AuthService {
        <<interface>>
        +register(RegisterRequest) AuthResponse
        +login(LoginRequest) AuthResponse
        +refresh(String) AuthResponse
        +getProfile(Long) UserResponse
    }

    class AuthServiceImpl {
        -UserRepository userRepository
        -AuthOtpRepository otpRepository
        -PasswordEncoder passwordEncoder
        -JwtUtil jwtUtil
        -AuthOtpEmailService otpEmailService
        -AuthEventProducer authEventProducer
    }

    class JwtAuthenticationFilter {
        +doFilterInternal(req,res,chain)
    }

    class JwtUtil {
        +generateToken(User) String
        +validateToken(String) boolean
        +extractUsername(String) String
    }

    class AuthEventProducer {
        +publishAccountStatusChanged(User) void
    }

    AuthController --> AuthService
    AdminController --> AuthService
    InternalUserController --> AuthService
    AuthService <|.. AuthServiceImpl
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> AuthOtpRepository
    AuthServiceImpl --> JwtUtil
    AuthServiceImpl --> AuthOtpEmailService
    AuthServiceImpl --> AuthEventProducer
    JwtAuthenticationFilter --> JwtUtil
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Angular as Angular SPA
    participant Gateway as API Gateway
    participant Auth as AuthController
    participant Service as AuthServiceImpl
    participant Users as UserRepository
    participant OTP as AuthOtpRepository
    participant Mail as SMTP / MailSender
    participant JWT as JwtUtil
    participant Kafka as Kafka Topic

    User->>Angular: Submit email/password
    Angular->>Gateway: POST /api/v1/auth/login
    Gateway->>Auth: Route public auth request
    Auth->>Service: login(LoginRequest)
    Service->>Users: findByEmail(email)
    Users-->>Service: User with password hash/status
    Service->>Service: BCrypt password match + active/suspended checks
    alt OTP required
        Service->>OTP: save hashed OTP + expiry
        Service->>Mail: send OTP email
        Service-->>Auth: OtpChallengeResponse
        Auth-->>Angular: 202 OTP required
        User->>Angular: Submit OTP
        Angular->>Gateway: POST /api/v1/auth/login/verify-otp
        Gateway->>Auth: Route verification
        Auth->>Service: verifyLoginOtp(VerifyOtpRequest)
        Service->>OTP: validate hash, expiry, attempts
    end
    Service->>JWT: generateToken(userId, role, email)
    JWT-->>Service: Signed JWT
    Service-->>Auth: AuthResponse
    Auth-->>Gateway: 200 OK + JWT + user
    Gateway-->>Angular: AuthResponse
    Angular->>Angular: Store token and current user

    opt Admin changes account status
        Auth->>Service: suspend/restore user
        Service->>Kafka: account.status.changed
    end
```

## Component Diagram
```mermaid
flowchart TD
    Client[Angular SPA] -->|/api/v1/auth/**| Gateway[API Gateway]
    Gateway -->|routes public/protected auth APIs| AuthController

    subgraph Auth_Service["auth-service :8081"]
        AuthController[AuthController]
        AdminController[AdminController]
        InternalUserController[InternalUserController]
        Security[SecurityConfig + JWT Filter]
        OAuth[OAuth2 Success Handler]
        Service[AuthServiceImpl]
        Jwt[JwtUtil]
        OtpEmail[AuthOtpEmailService]
        Producer[AuthEventProducer]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Persistence["MySQL schema"]
        Users[(users)]
        Otps[(auth_otps)]
    end

    SMTP[(SMTP server)]
    Kafka[(Kafka: account.status.changed)]
    Eureka[Eureka Registry]
    Admin[Spring Boot Admin]

    AuthController --> Service
    AdminController --> Service
    InternalUserController --> Service
    Security --> Jwt
    OAuth --> Service
    Service --> Users
    Service --> Otps
    Service --> Jwt
    Service --> OtpEmail
    OtpEmail --> SMTP
    Service --> Producer --> Kafka
    Auth_Service -->|registers| Eureka
    Auth_Service -->|actuator health| Admin
    AOP -.times controller/service methods.-> Service
    AOP -.writes.-> Logs
```
