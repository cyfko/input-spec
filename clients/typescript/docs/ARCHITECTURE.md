# Architecture Overview

## 🏗️ System Architecture Diagram

```mermaid
graph TB
    %% User Layer
    subgraph "Application Layer"
        APP[Your Application]
        FORM[Form Components]
        FIELD[Input Fields]
    end

    %% Framework Integration Layer
    subgraph "Framework Integration"
        ANGULAR[Angular HttpClient]
        AXIOS[Axios Instance]
        FETCH[Custom Fetch]
        ADAPTERS[Framework Adapters]
    end

    %% Protocol Layer
    subgraph "Protocol Layer"
        SPEC[InputFieldSpec]
        CONSTRAINT[ConstraintDescriptor]
        ENDPOINT[ValuesEndpoint]
    end

    %% Implementation Layer
    subgraph "Implementation Layer"
        VALIDATOR[FieldValidator]
        RESOLVER[ValuesResolver]
        HTTP[HttpClient Interface]
        CACHE[CacheProvider Interface]
    end

    %% Infrastructure Layer
    subgraph "Infrastructure Layer"
        FACTORY[HttpClientFactory]
        MEMORY[MemoryCacheProvider]
        API[External APIs]
        SERVER[Backend Services]
    end

    %% Connections
    APP --> FORM
    FORM --> FIELD
    FIELD --> SPEC
    FIELD --> VALIDATOR
    FIELD --> RESOLVER

    SPEC --> CONSTRAINT
    RESOLVER --> ENDPOINT
    RESOLVER --> HTTP
    RESOLVER --> CACHE

    ANGULAR --> ADAPTERS
    AXIOS --> ADAPTERS
    FETCH --> ADAPTERS
    ADAPTERS --> FACTORY
    FACTORY --> HTTP
    CACHE --> MEMORY
    HTTP --> API
    FETCH --> SERVER

    VALIDATOR --> SPEC
    VALIDATOR --> CONSTRAINT
```

## 🔄 Data Flow Diagram

```mermaid
sequenceDiagram
    participant UI as User Interface
    participant VAL as FieldValidator
    participant RES as ValuesResolver
    participant HTTP as HttpClient
    participant API as External API
    participant CACHE as CacheProvider

    %% Field Validation Flow
    UI->>VAL: validate(fieldSpec, value)
    VAL->>VAL: Check constraints
    VAL->>UI: ValidationResult

    %% Dynamic Values Flow
    UI->>RES: resolveValues(endpoint, options)
    RES->>CACHE: get(cacheKey)
    
    alt Cache Hit
        CACHE->>RES: Cached data
        RES->>UI: FetchValuesResult
    else Cache Miss
        RES->>HTTP: request(url, options)
        HTTP->>API: HTTP Request
        API->>HTTP: JSON Response
        HTTP->>RES: Parsed data
        RES->>CACHE: set(cacheKey, data)
        RES->>UI: FetchValuesResult
    end
```

## 🔌 Framework Integration Strategy

### HTTP Client Injection Architecture

The v2.0 architecture introduces a sophisticated HTTP client injection system that preserves existing framework infrastructure:

#### Design Principles

1. **Framework Agnostic Core**: The core validation logic never directly makes HTTP calls
2. **Adapter Pattern**: Framework-specific adapters translate between our interface and framework HTTP clients
3. **Interceptor Preservation**: Existing authentication, logging, and error handling interceptors continue to work
4. **Zero Breaking Changes**: Existing HTTP configurations are preserved

#### HTTP Client Factory

```typescript
// Automatic detection and adaptation
const httpClient = HttpClientFactory.createAngularAdapter(angularHttpClient);
const httpClient = HttpClientFactory.createAxiosAdapter(axiosInstance);
const httpClient = HttpClientFactory.createFetchAdapter(customConfig);
```

#### Benefits for Frontend Teams

- **Angular Teams**: Use Angular's HttpClient with all interceptors intact
- **React Teams**: Use existing Axios configurations without modification  
- **Vue Teams**: Integrate with existing HTTP client setups
- **Legacy Teams**: Custom fetch configuration with interceptors

### Cache Provider Integration

Similar adapter pattern for cache systems:

```typescript
// Framework cache integration
const cache = new FrameworkCacheAdapter(existingCacheSystem);
const cache = new MemoryCacheProvider(); // Default implementation
```

### Dependency Injection Examples

#### Angular Service
```typescript
@Injectable()
export class FieldValidationService {
  constructor(private httpClient: HttpClient) {
    const adapter = HttpClientFactory.createAngularAdapter(httpClient);
    this.valuesResolver = new ValuesResolver(adapter, cache);
  }
}
```

#### React Hook
```typescript
function useFieldValidation() {
  const httpClient = useMemo(() => 
    HttpClientFactory.createAxiosAdapter(axiosInstance), []
  );
  const valuesResolver = useMemo(() => 
    new ValuesResolver(httpClient, cache), []
  );
}
```

```mermaid
graph LR
    subgraph "Types Module"
        T1[InputFieldSpec]
        T2[ConstraintDescriptor] 
        T3[ValuesEndpoint]
        T4[ValidationResult]
    end

    subgraph "Validation Module"
        V1[FieldValidator]
        V2[validateSingleValue]
        V3[validateArray]
    end

    subgraph "Client Module"
        C1[ValuesResolver]
        C2[HttpClient Interface]
        C3[CacheProvider Interface]
        C4[FetchHttpClient]
        C5[MemoryCacheProvider]
    end

    T1 --> V1
    T2 --> V1
    T1 --> C1
    T3 --> C1
    C2 --> C4
    C3 --> C5
    C1 --> C2
    C1 --> C3
```