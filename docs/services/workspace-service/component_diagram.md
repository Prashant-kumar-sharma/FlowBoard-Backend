```mermaid
flowchart TD
    subgraph Workspace_Service
        WorkspaceController
        WorkspaceServiceImpl
        PaymentClient
    end
    subgraph Workspace_DB
        workspaces[(workspaces table)]
    end
    WorkspaceController --> WorkspaceServiceImpl
    WorkspaceServiceImpl --> workspaces
    WorkspaceServiceImpl --> PaymentClient
```
