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
