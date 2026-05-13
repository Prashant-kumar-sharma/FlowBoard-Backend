```mermaid
sequenceDiagram
    User->>LabelController: POST /labels
    LabelController->>LabelServiceImpl: createLabel()
    LabelServiceImpl->>LabelRepository: save()
    LabelRepository-->>LabelServiceImpl: Label
    LabelServiceImpl-->>LabelController: LabelDTO
```
