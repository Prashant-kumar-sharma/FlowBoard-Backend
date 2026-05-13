```mermaid
flowchart TD
    subgraph Label_Service
        LabelController
        LabelServiceImpl
    end
    subgraph Label_DB
        labels[(labels table)]
    end
    LabelController --> LabelServiceImpl
    LabelServiceImpl --> labels
```
