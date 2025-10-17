
layout: default
title: FAQ
nav_order: 7
description: "Frequently asked questions about the protocol and its implementations."
nav_exclude: true

[🇫🇷 French](../FAQ.md) | [🇬🇧 English](./FAQ.md)

# ❓ FAQ and Real-World Scenarios

*Answers to frequently asked questions with concrete client and server examples*


## 🎯 General Questions

### Q: Why use this protocol instead of client-side validation only?

**A:** Client-side only validation has several critical issues:

**❌ Problems with client-only approach:**
- Compromised security (easy to bypass)
- Code duplication across projects
- Inconsistency between teams
- Scattered maintenance

**✅ Protocol advantages:**
- **Single source of truth** – Rules are defined once on the server
- **Enhanced security** – Systematic server-side validation
- **Guaranteed consistency** – Same rules applied everywhere
- **Centralized maintenance** – A rule change propagates automatically

**Concrete example:**

```typescript
// ❌ Before: Duplicated and inconsistent validation
// Client 1 (React)
const validateEmail = (email) => /^[^@]+@[^@]+\.[^@]+$/.test(email);

// Client 2 (Vue)
const validateEmail = (email) => email.includes('@'); // 😱 Inconsistent!

// Server
const validateEmail = (email) => /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
```

```typescript
// ✅ After: Unified protocol
// Server – Single source of truth
const emailFieldSpec = {
  displayName: "Email",
  dataType: "STRING",
  required: true,
  constraints: [{
    name: "email",
    pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    errorMessage: "Invalid email format"
  }]
};

// All clients use the same specification
const validator = new FieldValidator();
const result = await validator.validate(emailFieldSpec, userEmail);

---

### Q: How do clients stay in sync with server-side rules?

**A:** The protocol is designed so that clients always fetch the latest field specifications from the server (via API or static file). This ensures that any rule change is instantly reflected on all clients, without manual updates or redeployment.

**Example:**

```js
// Client fetches the latest field spec at runtime
const fieldSpec = await fetch('/api/fields/email').then(r => r.json());
const result = await validator.validate(fieldSpec, userEmail);
```

---

## 🏢 Business Scenarios

### Q: Can the protocol handle complex business rules (e.g., conditional fields, cross-field validation)?

**A:** Yes! The protocol supports advanced constraints, including conditional logic and cross-field dependencies. You can define rules such as "Field B is required only if Field A has value X" or "Field C must be greater than Field D".

**Example:**

```json
{
  "name": "discountCode",
  "displayName": "Discount Code",
  "dataType": "STRING",
  "required": false,
  "constraints": [
    {
      "name": "requiredIf",
      "field": "hasDiscount",
      "value": true,
      "errorMessage": "Discount code required if discount is selected"
    }
  ]
}
```
```

### Q: How to handle complex business validations specific to my domain?

**A:** The protocol provides several extension mechanisms:

#### 1. **Custom constraints with ValuesEndpoint**

**🖥️ Server side:**
```java
@RestController
public class BusinessValidationController {
    
  @PostMapping("/api/validate/project-code")
  public ValidationResponse validateProjectCode(@RequestBody ValidateRequest request) {
    String projectCode = request.getValue();
        
    // Complex business logic
    boolean isValid = projectService.isCodeAvailable(projectCode) &&
             projectService.hasUserPermission(request.getUserId(), projectCode) &&
             projectService.isCodeFormatValid(projectCode);
        
    return ValidationResponse.builder()
      .isValid(isValid)
      .message(isValid ? "Code available" : "Invalid or unavailable project code")
      .build();
  }
    
  // Field specification with business validation
  @GetMapping("/api/fields/project-code")
  public InputFieldSpec getProjectCodeSpec() {
    return InputFieldSpec.builder("Project code", DataType.STRING)
      .required(true)
      .constraints(Arrays.asList(
        ConstraintDescriptor.builder()
          .name("format")
          .pattern("^[A-Z]{2,3}-\\d{4}$")
          .errorMessage("Format: XX-1234 or XXX-1234")
          .build(),
        ConstraintDescriptor.builder()
          .name("business_validation")
          .valuesEndpoint(ValuesEndpoint.builder()
            .uri("/api/validate/project-code")
            .method("POST")
            .build())
          .errorMessage("Invalid project code")
          .build()
      ))
      .build();
  }
}
```

**💻 Client side:**
```typescript
// The client automatically calls the validation endpoint
const projectCodeField = await loadFieldSpec('project-code');
const validationResult = await validator.validate(projectCodeField, 'AB-1234');

// Automatic sequence:
// 1. Pattern validation: ^[A-Z]{2,3}-\d{4}$ ✅
// 2. POST call to /api/validate/project-code with "AB-1234"
// 3. Server response validated or error returned
```

#### 2. **Custom validation plugin**

```typescript
// Plugin for domain-specific business validations
class ProjectManagementPlugin implements ProtocolPlugin {
  name = 'project-management';
  version = '1.0.0';
  
  customConstraints = new Map([
    ['project_budget_limit', new BudgetConstraintProcessor()],
    ['team_size_validation', new TeamSizeProcessor()],
    ['milestone_dependencies', new MilestoneProcessor()]
  ]);
  
  constructor(private projectService: ProjectService) {}
}

// Usage
const extensionManager = new ProtocolExtensionManager();
extensionManager.registerPlugin(new ProjectManagementPlugin(projectService));
```

### Q: How to optimize performance for forms with many fields?

**A:** Several optimization strategies are available:

#### 1. **Lazy validation and smart debouncing**

```typescript
// Advanced configuration with optimizations
const optimizedFormManager = new ProjectFormManager(httpClient, cache, {
  validationStrategy: 'lazy', // Validate only on blur or submit
  debouncing: {
    search: 300,    // 300ms for searches
    validation: 500, // 500ms for server validations
    adaptive: true   // Auto-adjust based on typing speed
  },
  batching: {
    enabled: true,
    maxBatchSize: 10,
    maxWaitTime: 200
  }
});
```

#### 2. **Cache stratified by volatility level**

```typescript
// Cache with adaptive TTL based on data type
const intelligentCache = new IntelligentCacheProvider({
  strategies: {
    'user_search': { ttl: 5 * 60 * 1000, priority: 'high' },     // 5min - user data
    'enum_values': { ttl: 60 * 60 * 1000, priority: 'medium' },  // 1h - static lists  
    'validation_results': { ttl: 30 * 1000, priority: 'low' }    // 30s - validation results
  },
  evictionPolicy: 'LFU' // Least Frequently Used for forms
});
```

#### 3. **Smart preloading**

```typescript
// Preloading based on user behavior analysis
class SmartFormPreloader {
  async preloadLikelyFields(currentField: string, userBehavior: UserBehaviorData) {
    const predictions = this.predictNextFields(currentField, userBehavior);
    
    // Preload in background the 3 most likely fields
    const preloadPromises = predictions.slice(0, 3).map(fieldName => 
      this.preloadFieldData(fieldName)
    );
    
    // Do not wait – run in background
    Promise.allSettled(preloadPromises);
  }
  
  private predictNextFields(current: string, behavior: UserBehaviorData): string[] {
    // ML simple basé sur les patterns d'usage
    const transitions = behavior.fieldTransitions[current] || [];
    return transitions
      .sort((a, b) => b.probability - a.probability)
      .map(t => t.nextField);
  }
}
```

## 🔧 Technical Questions

### Q: How to integrate the protocol with React Hook Form?

**A:** Here is a complete adapter for React Hook Form:

```typescript
// React Hook Form adapter
import { useForm, Controller } from 'react-hook-form';
import { FieldValidator, InputFieldSpec } from '@cyfko/input-spec';

export function useProtocolForm<T extends Record<string, any>>(
  fieldSpecs: Record<keyof T, InputFieldSpec>,
  options?: UseProtocolFormOptions
) {
  const validator = new FieldValidator();
  const resolver = useMemo(() => createProtocolResolver(fieldSpecs, validator), [fieldSpecs]);
  
  const form = useForm<T>({
    resolver,
    mode: options?.validationMode || 'onBlur',
    ...options?.reactHookFormOptions
  });
  
  return {
    ...form,
    // Extended protocol methods
    validateField: async (fieldName: keyof T) => {
      const fieldSpec = fieldSpecs[fieldName];
      const value = form.getValues(fieldName);
      const result = await validator.validate(fieldSpec, value);
      
      if (!result.isValid) {
        form.setError(fieldName, {
          message: result.errors[0]?.message || 'Validation error'
        });
      }
      
      return result.isValid;
    },
    
    // Value search for fields with ValuesEndpoint
    searchValues: async (fieldName: keyof T, query: string) => {
      const fieldSpec = fieldSpecs[fieldName];
      // v2: valuesEndpoint is at the field level (not in a constraint anymore)
      if (!fieldSpec.valuesEndpoint) {
        throw new Error(`No search endpoint (valuesEndpoint) for ${String(fieldName)}`);
      }

      return resolver.resolveValues(fieldSpec.valuesEndpoint, { search: query });
    }
  };
}

// Smart field component with search
const SmartSelectField: React.FC<{
  name: string;
  fieldSpec: InputFieldSpec;
  control: Control<any>;
}> = ({ name, fieldSpec, control }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [options, setOptions] = useState<ValueAlias[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  
  const debouncedSearch = useDebounce(searchQuery, 300);
  
  // Auto-search when query changes
  useEffect(() => {
    if (debouncedSearch.length >= 2) {
      searchValues(debouncedSearch);
    }
  }, [debouncedSearch]);
  
  const searchValues = async (query: string) => {
    setIsLoading(true);
    try {
      // v2: accès direct au fieldSpec.valuesEndpoint
      if (!fieldSpec.valuesEndpoint) {
        throw new Error('Ce champ ne définit pas de valuesEndpoint (v2)');
      }
      const resolver = new ValuesResolver(httpClient, cache);
      const result = await resolver.resolveValues(fieldSpec.valuesEndpoint, {
        search: query,
        limit: fieldSpec.valuesEndpoint.requestParams?.defaultLimit || 20
      });
      setOptions(result.values);
    } catch (error) {
      console.error('Erreur de recherche:', error);
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <Controller
      name={name}
      control={control}
      rules={`{
        required: fieldSpec.required ? 'Ce champ est obligatoire' : false,
        validate: async (value) => {
          const validator = new FieldValidator();
          const result = await validator.validate(fieldSpec, value);
          return result.isValid || result.errors[0]?.message;
        }
      }`}
      render={`{ field, fieldState }) => (`}
        <div className="smart-select-field">
          <label>{fieldSpec.displayName}</label>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`Rechercher ${fieldSpec.displayName.toLowerCase()}...`}
            className={fieldState.error ? 'error' : ''}
          />
          
          {isLoading && <div className="loading">Recherche...</div>}
          
          {options.length > 0 && (
            <div className="options-dropdown">
              {options.map(option => (
                <div
                  key={option.value}
                  className="option"
                  onClick={() => {
                    field.onChange(option.value);
                    setSearchQuery(option.label);
                  }}
                >
                  {option.label}
                </div>
              ))}
            </div>
          )}
          
          {fieldState.error && (
            <div className="error-message">{fieldState.error.message}</div>
          )}
        </div>
      )}
    />
  );
};

// Usage dans un composant
const ProjectForm: React.FC = () => {
  const fieldSpecs = {
    projectName: await loadFieldSpec('project-name'),
    projectLead: await loadFieldSpec('project-lead'),
    teamMembers: await loadFieldSpec('team-members')
  };
  
  const { control, handleSubmit, validateField, searchValues } = useProtocolForm(fieldSpecs);
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <SmartSelectField 
        name="projectLead" 
        fieldSpec={fieldSpecs.projectLead}
        control={control}
      />
      {/* ... autres champs */}
    </form>
  );
};
```

### Q: Comment gérer les formulaires multi-étapes avec des validations conditionnelles ?

**R:** Voici une approche avec orchestrateur d'état :

```typescript
// Gestionnaire de formulaire multi-étapes
class MultiStepFormOrchestrator {
  private currentStep = 0;
  private stepValidations = new Map<number, ValidationResult>();
  private formData = new Map<string, any>();
  
  constructor(
    private steps: FormStep[],
    private validator: FieldValidator
  ) {}
  
  async validateCurrentStep(): Promise<boolean> {
    const step = this.steps[this.currentStep];
    const stepData = this.getStepData(step.fields);
    
    let isStepValid = true;
    const stepErrors = new Map<string, ValidationError[]>();
    
    for (const fieldName of step.fields) {
      const fieldSpec = await this.loadFieldSpec(fieldName, this.getContext());
      const value = stepData[fieldName];
      
      const result = await this.validator.validate(fieldSpec, value);
      
      if (!result.isValid) {
        isStepValid = false;
        stepErrors.set(fieldName, result.errors);
      }
    }
    
    this.stepValidations.set(this.currentStep, {
      isValid: isStepValid,
      errors: Array.from(stepErrors.values()).flat()
    });
    
    return isStepValid;
  }
  
  async nextStep(): Promise<boolean> {
    const isCurrentStepValid = await this.validateCurrentStep();
    
    if (!isCurrentStepValid) {
      return false;
    }
    
    // Logique conditionnelle pour déterminer le prochain step
    const nextStepIndex = await this.determineNextStep();
    
    if (nextStepIndex < this.steps.length) {
      this.currentStep = nextStepIndex;
      
      // Précharger les données du prochain step
      await this.preloadStepData(this.currentStep);
      
      return true;
    }
    
    return false; // Fin du formulaire
  }
  
  private async determineNextStep(): Promise<number> {
    const currentStepData = this.getStepData(this.steps[this.currentStep].fields);
    
    // Exemple de logique conditionnelle
    if (this.currentStep === 0 && currentStepData.projectType === 'RESEARCH') {
      return 2; // Ignorer l'étape 1 pour les projets de recherche
    }
    
    if (this.currentStep === 1 && currentStepData.teamSize === 'SOLO') {
      return 3; // Ignorer l'étape de sélection d'équipe
    }
    
    return this.currentStep + 1;
  }
  
  private getContext(): Record<string, any> {
    // Construire le contexte avec toutes les données saisies
    const context: Record<string, any> = {};
    this.formData.forEach((value, key) => {
      context[key] = value;
    });
    return context;
  }
}

// Configuration des étapes
const projectFormSteps: FormStep[] = [
  {
    id: 'basic_info',
    title: 'Informations de base',
    fields: ['projectName', 'projectType', 'description'],
    validation: 'immediate'
  },
  {
    id: 'team_setup',
    title: 'Configuration équipe',
    fields: ['projectLead', 'teamMembers'],
    conditionalDisplay: (data) => data.projectType !== 'SOLO',
    validation: 'on_complete'
  },
  {
    id: 'budget_timeline',
    title: 'Budget et planning',
    fields: ['budget', 'startDate', 'deadline'],
    conditionalDisplay: (data) => data.projectType !== 'RESEARCH',
    validation: 'on_complete'
  }
];
```

### Q: Comment sécuriser les validations côté serveur ?

**R:** Implémentation sécurisée avec validation en profondeur :

**🖥️ Côté serveur Java - Validation sécurisée :**

```java
@Service
@Validated
public class SecureValidationService {
    
    private static final int MAX_VALIDATION_TIME_MS = 5000;
    private static final int MAX_STRING_LENGTH = 10000;
    private static final int MAX_ARRAY_SIZE = 1000;
    
    @Autowired
    private SecurityContext securityContext;
    
    @Autowired
    private AuditService auditService;
    
    @PreAuthorize("hasPermission(#fieldName, 'VALIDATE')")
    public ValidationResult validateSecurely(
            @NotNull String fieldName,
            @NotNull InputFieldSpec fieldSpec,
            Object value,
            @AuthenticationPrincipal UserDetails user) {
        
        // Audit de la tentative de validation
        auditService.logValidationAttempt(user.getUsername(), fieldName, value);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Validation des limites de sécurité
            validateSecurityLimits(value, fieldSpec);
            
            // 2. Sanitisation préventive
            Object sanitizedValue = sanitizeInput(value, fieldSpec.getDataType());
            
            // 3. Validation du contexte de sécurité
            validateSecurityContext(fieldSpec, user);
            
            // 4. Validation métier avec timeout
            ValidationResult result = executeValidationWithTimeout(
                fieldSpec, 
                sanitizedValue, 
                MAX_VALIDATION_TIME_MS
            );
            
            // 5. Audit du résultat
            auditService.logValidationResult(
                user.getUsername(), 
                fieldName, 
                result.isValid(),
                System.currentTimeMillis() - startTime
            );
            
            return result;
            
        } catch (SecurityException e) {
            auditService.logSecurityViolation(user.getUsername(), fieldName, e.getMessage());
            throw e;
        } catch (ValidationTimeoutException e) {
            auditService.logValidationTimeout(user.getUsername(), fieldName);
            throw new ValidationException("Validation timeout - opération trop complexe");
        }
    }
    
    private void validateSecurityLimits(Object value, InputFieldSpec fieldSpec) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() > MAX_STRING_LENGTH) {
                throw new SecurityException("Input trop long: " + str.length());
            }
            
            // Vérifier les caractères suspects
            if (containsSuspiciousPatterns(str)) {
                throw new SecurityException("Pattern suspect détecté");
            }
        }
        
        if (fieldSpec.isExpectMultipleValues() && value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() > MAX_ARRAY_SIZE) {
                throw new SecurityException("Array trop grand: " + list.size());
            }
        }
    }
    
    private Object sanitizeInput(Object value, DataType dataType) {
        if (dataType == DataType.STRING && value instanceof String) {
            String str = (String) value;
            
            // Échapper les caractères HTML/JS dangereux
            str = HtmlUtils.htmlEscape(str);
            
            // Retirer les caractères de contrôle
            str = str.replaceAll("[\\p{Cntrl}]", "");
            
            // Normaliser les espaces
            str = str.trim().replaceAll("\\s+", " ");
            
            return str;
        }
        
        return value;
    }
    
    private void validateSecurityContext(InputFieldSpec fieldSpec, UserDetails user) {
        // Vérifier que l'utilisateur a le droit de valider ce champ
        String fieldCategory = extractFieldCategory(fieldSpec.getDisplayName());
        
        if ("budget".equals(fieldCategory) && !hasRole(user, "BUDGET_MANAGER")) {
            throw new SecurityException("Permissions insuffisantes pour: " + fieldCategory);
        }
        
        if ("sensitive".equals(fieldCategory) && !hasRole(user, "SENSITIVE_DATA_ACCESS")) {
            throw new SecurityException("Accès aux données sensibles non autorisé");
        }
    }
    
    private ValidationResult executeValidationWithTimeout(
            InputFieldSpec fieldSpec, 
            Object value, 
            long timeoutMs) throws ValidationTimeoutException {
        
        CompletableFuture<ValidationResult> future = CompletableFuture.supplyAsync(() -> {
            FieldValidator validator = new FieldValidator();
            return validator.validate(fieldSpec, value);
        });
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ValidationTimeoutException("Validation timeout après " + timeoutMs + "ms");
        } catch (InterruptedException | ExecutionException e) {
            throw new ValidationException("Erreur de validation: " + e.getMessage());
        }
    }
    
    private boolean containsSuspiciousPatterns(String input) {
        // Patterns d'injection courants
        String[] suspiciousPatterns = {
            "(?i)<script",           // XSS
            "(?i)javascript:",       // XSS 
            "(?i)on\\w+\\s*=",      // Event handlers
            "(?i)(union|select|insert|update|delete)\\s+", // SQL injection
            "\\.\\./",               // Path traversal
            "<%",                    // Server-side includes
            "\\$\\{",               // Expression language injection
        };
        
        for (String pattern : suspiciousPatterns) {
            if (input.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        
        return false;
    }
}

// Configuration de sécurité Spring
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ValidationSecurityConfig {
    
    @Bean
    public PermissionEvaluator validationPermissionEvaluator() {
        return new ValidationPermissionEvaluator();
    }
}

// Évaluateur de permissions personnalisé
public class ValidationPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (!(targetDomainObject instanceof String) || !(permission instanceof String)) {
            return false;
        }
        
        String fieldName = (String) targetDomainObject;
        String permissionName = (String) permission;
        
        if ("VALIDATE".equals(permissionName)) {
            return canValidateField(auth, fieldName);
        }
        
        return false;
    }
    
    private boolean canValidateField(Authentication auth, String fieldName) {
        // Logique de permission basée sur le nom du champ et les rôles utilisateur
        if (fieldName.contains("budget") || fieldName.contains("financial")) {
            return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BUDGET_MANAGER"));
        }
        
        if (fieldName.contains("personal") || fieldName.contains("sensitive")) {
            return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DATA_PROTECTION_OFFICER"));
        }
        
        // Par défaut, tous les utilisateurs authentifiés peuvent valider les champs basiques
        return auth.isAuthenticated();
    }
}
```

## 🌐 Real-World Usage Scenarios

### Scenario 1: E-commerce – Order Form

**Context:** Online store with real-time shipping address validation.

**🖥️ Server side:**
```java
@RestController
public class CheckoutController {
    
    @GetMapping("/api/fields/shipping-address")
    public InputFieldSpec getShippingAddressSpec() {
        return InputFieldSpec.builder("Shipping address", DataType.STRING)
            .required(true)
            .constraints(Arrays.asList(
                // Address format validation
                ConstraintDescriptor.builder()
                    .name("address_format")
                    .min(10)
                    .max(200)
                    .errorMessage("Address must be between 10 and 200 characters")
                    .build(),
                
                // Real-time geographic validation
                ConstraintDescriptor.builder()
                    .name("delivery_zone")
                    .valuesEndpoint(ValuesEndpoint.builder()
                        .uri("/api/validate/delivery-address")
                        .method("POST")
                        .debounceMs(1000) // 1 second to avoid too many calls
                        .responseMapping(ResponseMapping.builder()
                            .dataField("isDeliverable")
                            .build())
                        .build())
                    .errorMessage("Delivery not available to this address")
                    .build()
            ))
            .build();
    }
    
    @PostMapping("/api/validate/delivery-address")
    public DeliveryValidationResponse validateDeliveryAddress(
            @RequestBody AddressValidationRequest request) {
        
        // Integration with geocoding service (Google Maps, etc.)
        GeocodingResult result = geocodingService.validateAddress(request.getAddress());
        
        if (!result.isValid()) {
            return DeliveryValidationResponse.builder()
                .isDeliverable(false)
                .message("Address not found")
                .build();
        }
        
        // Check delivery zones
        boolean inDeliveryZone = deliveryService.isInDeliveryZone(
            result.getLatitude(), 
            result.getLongitude()
        );
        
        // Calculate delivery fee
        BigDecimal deliveryFee = deliveryService.calculateDeliveryFee(
            result.getLatitude(), 
            result.getLongitude()
        );
        
        return DeliveryValidationResponse.builder()
            .isDeliverable(inDeliveryZone)
            .message(inDeliveryZone ? "Delivery available" : "Zone not covered")
            .deliveryFee(deliveryFee)
            .estimatedDeliveryTime(deliveryService.getEstimatedDeliveryTime(result))
            .build();
    }
}
```

**💻 Client side:**
```typescript
// React component for shipping address
const ShippingAddressField: React.FC = () => {
  const [address, setAddress] = useState('');
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [deliveryInfo, setDeliveryInfo] = useState<DeliveryInfo | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  
  const debouncedAddress = useDebounce(address, 1000);
  
  useEffect(() => {
    if (debouncedAddress.length >= 10) {
      validateAddress(debouncedAddress);
    }
  }, [debouncedAddress]);
  
  const validateAddress = async (addressValue: string) => {
    setIsValidating(true);
    
    try {
      const fieldSpec = await loadFieldSpec('shipping-address');
      const validator = new FieldValidator();
      const result = await validator.validate(fieldSpec, addressValue);
      
      setValidationResult(result);
      
      // If server-side validation succeeded, extract delivery info
      if (result.isValid && result.metadata?.deliveryInfo) {
        setDeliveryInfo(result.metadata.deliveryInfo);
      }
      
    } catch (error) {
      console.error('Address validation error:', error);
    } finally {
      setIsValidating(false);
    }
  };
  
  return (
    <div className="shipping-address-field">
      <label htmlFor="address">Shipping address *</label>
      
      <textarea
        id="address"
        value={address}
        onChange={(e) => setAddress(e.target.value)}
        placeholder="Enter your full address..."
        className={validationResult && !validationResult.isValid ? 'error' : ''}
        rows={3}
      />
      
      {isValidating && (
        <div className="validation-status">
          <span className="spinner" />
          Validating address...
        </div>
      )}
      
      {validationResult && !validationResult.isValid && (
        <div className="error-messages">
          {validationResult.errors.map((error, index) => (
            <div key={index} className="error-message">
              {error.message}
            </div>
          ))}
        </div>
      )}
      
      {deliveryInfo && validationResult?.isValid && (
        <div className="delivery-info">
          <div className="delivery-available">
            ✅ Delivery available
          </div>
          <div className="delivery-details">
            <span>Delivery fee: {deliveryInfo.fee}€</span>
            <span>Estimated time: {deliveryInfo.estimatedTime}</span>
          </div>
        </div>
      )}
    </div>
  );
};
```

### Scenario 2: HR Application – Hiring Form

**Context:** Candidate validation with external API integration and complex business rules.

**🖥️ Server side:**
```java
@RestController  
public class HiringController {
    
    @Autowired
    private BackgroundCheckService backgroundCheckService;
    
    @Autowired
    private CompetencyService competencyService;
    
    @GetMapping("/api/fields/candidate-skills")
    public InputFieldSpec getCandidateSkillsSpec(
            @RequestParam String position,
            @RequestParam String seniority) {
        
        // Spécification dynamique selon le poste
        List<ConstraintDescriptor> constraints = new ArrayList<>();
        
        // Compétences requises selon le poste
        constraints.add(ConstraintDescriptor.builder()
            .name("required_skills")
            .min(getMinSkillsForPosition(position, seniority))
            .max(20)
            .valuesEndpoint(ValuesEndpoint.builder()
                .uri("/api/skills/search")
                .searchField("name")
                .paginationStrategy(PaginationStrategy.PAGE_NUMBER)
                .requestParams(RequestParams.builder()
                    .searchParam("query")
                    .pageParam("page")
                    .limitParam("limit")
                    .defaultLimit(15)
                    .additionalParam("position", position) // Filtrage par poste
                    .additionalParam("minLevel", getSeniorityLevel(seniority))
                    .build())
                .responseMapping(ResponseMapping.builder()
                    .dataField("skills")
                    .totalField("total")
                    .hasNextField("hasNext")
                    .build())
                .cacheStrategy(CacheStrategy.LONG_TERM)
                .build())
            .errorMessage("Sélectionnez au moins " + getMinSkillsForPosition(position, seniority) + " compétences")
            .build());
        
        // Validation des niveaux de compétence
        constraints.add(ConstraintDescriptor.builder()
            .name("competency_validation")
            .valuesEndpoint(ValuesEndpoint.builder()
                .uri("/api/validate/competencies")
                .method("POST")
                .build())
            .errorMessage("Niveau de compétences insuffisant pour ce poste")
            .build());
        
        return InputFieldSpec.builder("Compétences", DataType.STRING)
            .expectMultipleValues(true)
            .required(true)
            .constraints(constraints)
            .build();
    }
    
    @PostMapping("/api/validate/competencies")
    public CompetencyValidationResponse validateCompetencies(
            @RequestBody CompetencyValidationRequest request) {
        
        // Analyser les compétences sélectionnées
        List<String> selectedSkills = request.getSkills();
        String position = request.getPosition();
        String seniority = request.getSeniority();
        
        // Calcul du score de correspondance
        CompetencyMatchResult match = competencyService.calculateMatch(
            selectedSkills, position, seniority
        );
        
        boolean isQualified = match.getOverallScore() >= getMinScoreForPosition(position, seniority);
        
        return CompetencyValidationResponse.builder()
            .isValid(isQualified)
            .overallScore(match.getOverallScore())
            .matchingSkills(match.getMatchingSkills())
            .missingCriticalSkills(match.getMissingCriticalSkills())
            .recommendations(match.getRecommendations())
            .message(isQualified ? 
                "Profil correspondant au poste" : 
                "Compétences insuffisantes pour ce niveau")
            .build();
    }
    
    @GetMapping("/api/fields/background-check")
    public InputFieldSpec getBackgroundCheckSpec() {
        return InputFieldSpec.builder("Vérification des antécédents", DataType.STRING)
            .required(true)
            .constraints(Arrays.asList(
                ConstraintDescriptor.builder()
                    .name("ssn_format")
                    .pattern("^\\d{3}-\\d{2}-\\d{4}$")
                    .errorMessage("Format SSN: XXX-XX-XXXX")
                    .build(),
                
                ConstraintDescriptor.builder()
                    .name("background_verification")
                    .valuesEndpoint(ValuesEndpoint.builder()
                        .uri("/api/verify/background")
                        .method("POST")
                        .debounceMs(2000) // Plus long pour les vérifications lourdes
                        .build())
                    .errorMessage("Échec de la vérification des antécédents")
                    .build()
            ))
            .build();
    }
    
    @PostMapping("/api/verify/background")  
    public BackgroundCheckResponse verifyBackground(
            @RequestBody BackgroundCheckRequest request) {
        
        try {
            // Appel API externe sécurisé pour vérification
            BackgroundCheckResult result = backgroundCheckService.performCheck(
                request.getSsn(),
                request.getFullName(),
                request.getDateOfBirth()
            );
            
            boolean isPassed = result.getCriminalRecord().isEmpty() && 
                              result.getCreditScore() >= 650 &&
                              result.getEmploymentHistory().isVerified();
            
            return BackgroundCheckResponse.builder()
                .isPassed(isPassed)
                .criminalRecordClear(result.getCriminalRecord().isEmpty())
                .creditScoreAcceptable(result.getCreditScore() >= 650)
                .employmentVerified(result.getEmploymentHistory().isVerified())
                .message(isPassed ? 
                    "Vérification réussie" : 
                    "Problème détecté lors de la vérification")
                .build();
                
        } catch (BackgroundCheckException e) {
            return BackgroundCheckResponse.builder()
                .isPassed(false)
                .message("Impossible de vérifier les antécédents: " + e.getMessage())
                .build();
        }
    }
}
```

**💻 Côté client :**
```typescript
// Composant de sélection de compétences avec scoring en temps réel
const CandidateSkillsSelector: React.FC<{
  position: string;
  seniority: string;
  onScoreChange: (score: CompetencyScore) => void;
}> = ({ position, seniority, onScoreChange }) => {
  
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);
  const [availableSkills, setAvailableSkills] = useState<ValueAlias[]>([]);
  const [competencyScore, setCompetencyScore] = useState<CompetencyScore | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const formManager = useFormManager();
  
  // Charger les compétences disponibles selon le poste
  useEffect(() => {
    loadSkillsForPosition();
  }, [position, seniority]);
  
  // Valider les compétences sélectionnées
  useEffect(() => {
    if (selectedSkills.length > 0) {
      validateSelectedSkills();
    }
  }, [selectedSkills]);
  
  const loadSkillsForPosition = async () => {
    try {
      const fieldSpec = await formManager.loadFieldSpec('candidate-skills', {
        position,
        seniority
      });
      // v2: plus de recherche du valuesEndpoint dans une contrainte
      if (fieldSpec.valuesEndpoint) {
        const result = await formManager.searchValues('candidate-skills', '');
        setAvailableSkills(result.values);
      }
    } catch (error) {
      console.error('Erreur chargement compétences:', error);
    }
  };
  
  const searchSkills = async (query: string) => {
    if (query.length < 2) return;
    
    setIsLoading(true);
    try {
      const result = await formManager.searchValues('candidate-skills', query);
      setAvailableSkills(result.values);
    } catch (error) {
      console.error('Erreur recherche compétences:', error);
    } finally {
      setIsLoading(false);
    }
  };
  
  const validateSelectedSkills = async () => {
    try {
      const response = await fetch('/api/validate/competencies', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          skills: selectedSkills,
          position,
          seniority
        })
      });
      
      const result: CompetencyValidationResponse = await response.json();
      
      const score: CompetencyScore = {
        overall: result.overallScore,
        matching: result.matchingSkills?.length || 0,
        missing: result.missingCriticalSkills?.length || 0,
        isQualified: result.isValid
      };
      
      setCompetencyScore(score);
      onScoreChange(score);
      
    } catch (error) {
      console.error('Erreur validation compétences:', error);
    }
  };
  
  const addSkill = (skill: ValueAlias) => {
    if (!selectedSkills.includes(skill.value)) {
      setSelectedSkills([...selectedSkills, skill.value]);
    }
    setSearchQuery('');
  };
  
  const removeSkill = (skillValue: string) => {
    setSelectedSkills(selectedSkills.filter(s => s !== skillValue));
  };
  
  return (
    <div className="candidate-skills-selector">
      <div className="skill-search">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            searchSkills(e.target.value);
          }}
          placeholder="Rechercher une compétence..."
        />
        
        {isLoading && <div className="loading">Recherche...</div>}
        
        {availableSkills.length > 0 && searchQuery && (
          <div className="skills-dropdown">
            {availableSkills.map(skill => (
              <div
                key={skill.value}
                className="skill-option"
                onClick={() => addSkill(skill)}
              >
                {skill.label}
              </div>
            ))}
          </div>
        )}
      </div>
      
      <div className="selected-skills">
        <h4>Compétences sélectionnées</h4>
        {selectedSkills.map(skillValue => {
          const skill = availableSkills.find(s => s.value === skillValue);
          return (
            <div key={skillValue} className="selected-skill">
              <span>{skill?.label || skillValue}</span>
              <button onClick={() => removeSkill(skillValue)}>×</button>
            </div>
          );
        })}
      </div>
      
      {competencyScore && (
        <div className={`competency-score ${competencyScore.isQualified ? 'qualified' : 'not-qualified'}`}>
          <h4>Score de correspondance: {competencyScore.overall}%</h4>
          <div className="score-details">
            <span>✅ Compétences correspondantes: {competencyScore.matching}</span>
            <span>❌ Compétences critiques manquantes: {competencyScore.missing}</span>
          </div>
          <div className={`qualification-status ${competencyScore.isQualified ? 'pass' : 'fail'}`}>
            {competencyScore.isQualified ? '✅ Candidat qualifié' : '❌ Compétences insuffisantes'}
          </div>
        </div>
      )}
    </div>
  );
};
```

## 🔚 Conclusion

This FAQ demonstrates the versatility of the Dynamic Input Field Specification Protocol through concrete scenarios. Each example shows how the protocol adapts to specific business needs while maintaining a consistent architecture.

### Key takeaways:

1. **🔄 Seamless client/server interaction** – The protocol automatically orchestrates exchanges
2. **🎯 Integrated business validation** – Complex rules are centralized on the server
3. **⚡ Optimized performance** – Native debouncing, caching, and batching
4. **🔒 Enhanced security** – Validation and sanitization at every level
5. **🧩 Extensibility** – Easily adapts to specific requirements

The protocol transforms the complexity of dynamic forms into a simple, standardized API, allowing teams to focus on business logic rather than technical plumbing.

---

*Have more questions? [Start a discussion](../../discussions) or check out the [full examples](../impl/)*