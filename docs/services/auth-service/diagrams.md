# Auth Service Diagrams

## ER Diagram
```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email "UNIQUE"
        VARCHAR password_hash
        VARCHAR role
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class AuthController {
        +login(req: LoginRequest) TokenResponse
        +register(req: RegisterRequest) UserDTO
    }
    class AuthServiceImpl {
        -UserRepository userRepository
        -JwtUtil jwtUtil
    }
    class UserRepository {
        <<interface>>
        +findByEmail(email: String) Optional~User~
    }
    AuthController --> AuthServiceImpl
    AuthServiceImpl --> UserRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>Gateway: POST /api/v1/auth/login
    Gateway->>AuthController: route request
    AuthController->>AuthServiceImpl: login(req)
    AuthServiceImpl->>UserRepository: findByEmail(email)
    UserRepository-->>AuthServiceImpl: User
    AuthServiceImpl->>JwtUtil: generateToken(User)
    JwtUtil-->>AuthServiceImpl: JWT
    AuthServiceImpl-->>AuthController: TokenResponse
    AuthController-->>Gateway: 200 OK + JWT
    Gateway-->>User: JWT Token
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Auth_Service
        AuthController
        AuthServiceImpl
        JwtUtil
    end
    subgraph Auth_DB
        users[(users table)]
    end
    AuthController --> AuthServiceImpl
    AuthServiceImpl --> users
```
