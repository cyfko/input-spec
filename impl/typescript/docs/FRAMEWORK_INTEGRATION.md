# Framework Integration: Frontend Consuming Backend Specs

**This guide shows how frontend frameworks consume InputFieldSpec from backend APIs**

Stop hardcoding validation rules! Let your backend define field specifications and your frontend frameworks consume them seamlessly.

## The Pattern: Backend Defines, Frontend Consumes

### 1. Backend API Returns Field Specs
```typescript
// Your backend API (Express.js example)
app.get('/api/form-fields/user-profile', (req, res) => {
  const userRole = req.user.role;
  
  // Different validation rules based on user role
  const emailField: InputFieldSpec = {
    displayName: "Email Address",
    dataType: "STRING",
    required: true,
    constraints: [
      { name: "email", type: "email", message: "Invalid email format" },
      { 
        name: "maxLength", 
        type: "maxLength", 
        value: userRole === 'premium' ? 200 : 50,
        message: `Email too long (max ${userRole === 'premium' ? 200 : 50} chars)`
      }
    ]
  };

  res.json({ fields: [emailField] });
});
```

### 2. Frontend Fetches and Uses Specs
```typescript
// Frontend fetches field definition from backend
const fieldSpecs = await fetch('/api/form-fields/user-profile')
  .then(r => r.json());

// Validate using backend-defined rules
const validator = new FieldValidator();
const result = validator.validate(userInput, fieldSpecs.fields[0]);
```

## Angular Integration

### Real-World Angular Service

```typescript
// user-form.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  FieldValidator, 
  ValuesResolver, 
  MemoryCacheProvider,
  HttpClientFactory,
  InputFieldSpec 
} from 'input-spec';

@Injectable({
  providedIn: 'root'
})
export class UserFormService {
  private validator: FieldValidator;
  private valuesResolver: ValuesResolver;

  constructor(private http: HttpClient) {
    // ✅ Preserves your Angular interceptors and DI!
    const httpClientAdapter = HttpClientFactory.createAngularAdapter(this.http);
    const cacheProvider = new MemoryCacheProvider();
    
    this.validator = new FieldValidator();
    this.valuesResolver = new ValuesResolver(httpClientAdapter, cacheProvider);
  }

  // Get form fields from YOUR backend
  getFormFields(formType: string): Observable<{fields: InputFieldSpec[]}> {
    return this.http.get<{fields: InputFieldSpec[]}>(`/api/forms/${formType}/fields`);
  }

  // Validate using backend-defined rules
  validateField(fieldSpec: InputFieldSpec, value: any) {
    return this.validator.validate(fieldSpec, value);
  }

  async getFieldValues(endpoint: ValuesEndpoint, options?: FetchValuesOptions) {
    return this.valuesResolver.resolveValues(endpoint, options);
  }
}
```

### With Custom Angular Interceptors

```typescript
// auth.interceptor.ts
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // ✅ This interceptor will work with our field validation requests!
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${this.getToken()}`)
    });
    return next.handle(authReq);
  }

  private getToken(): string {
    return localStorage.getItem('authToken') || '';
  }
}
```

```typescript
// app.module.ts
import { HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ]
})
export class AppModule { }
```

### Usage in Angular Component

```typescript
// user-form.component.ts
import { Component } from '@angular/core';
import { FieldValidationService } from './field-validation.service';
import { InputFieldSpec } from 'input-spec';

@Component({
  selector: 'app-user-form',
  template: `
    <form (ngSubmit)="onSubmit()">
      <input 
        [(ngModel)]="email" 
        (blur)="validateEmail()"
        [class.error]="emailError"
      />
      <div *ngIf="emailError" class="error-message">{{ emailError }}</div>
      
      <select [(ngModel)]="country" (focus)="loadCountries()">
        <option *ngFor="let country of countries" [value]="country.value">
          {{ country.label }}
        </option>
      </select>
    </form>
  `
})
export class UserFormComponent {
  email = '';
  country = '';
  emailError = '';
  countries: ValueAlias[] = [];

  private emailFieldSpec: InputFieldSpec = {
    displayName: 'Email',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true,
    constraints: [
      {
        name: 'email',
        format: 'email',
        errorMessage: 'Please enter a valid email address'
      }
    ]
  };

  constructor(private fieldValidation: FieldValidationService) {}

  async validateEmail() {
    const result = await this.fieldValidation.validateField(this.emailFieldSpec, this.email);
    this.emailError = result.isValid ? '' : result.errors[0]?.message || 'Invalid email';
  }

  async loadCountries() {
    const endpoint: ValuesEndpoint = {
      uri: '/api/countries',
      responseMapping: {
        dataField: 'data',
        hasNextField: 'hasNext'
      }
    };

    const result = await this.fieldValidation.getFieldValues(endpoint);
    this.countries = result.values;
  }
}
```

## React Integration

### With Axios and Custom Configuration

```typescript
// http-client.ts
import axios, { AxiosInstance } from 'axios';
import { HttpClientFactory, MemoryCacheProvider } from 'input-spec';

// ✅ Create Axios instance with your custom configuration
const axiosInstance: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  timeout: 10000,
});

// Add request interceptor for authentication
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Add response interceptor for error handling
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ✅ Use your configured Axios instance with our library
export const httpClient = HttpClientFactory.createAxiosAdapter(axiosInstance);
export const cacheProvider = new MemoryCacheProvider();
```

```typescript
// field-validation.hook.ts
import { useCallback, useMemo } from 'react';
import { FieldValidator, ValuesResolver } from 'input-spec';
import { httpClient, cacheProvider } from './http-client';

export function useFieldValidation() {
  const validator = useMemo(() => new FieldValidator(), []);
  const valuesResolver = useMemo(
    () => new ValuesResolver(httpClient, cacheProvider),
    []
  );

  const validateField = useCallback(
    async (fieldSpec: InputFieldSpec, value: any) => {
      return validator.validate(fieldSpec, value);
    },
    [validator]
  );

  const getFieldValues = useCallback(
    async (endpoint: ValuesEndpoint, options?: FetchValuesOptions) => {
      return valuesResolver.resolveValues(endpoint, options);
    },
    [valuesResolver]
  );

  return { validateField, getFieldValues };
}
```

```typescript
// UserForm.tsx
import React, { useState, useEffect } from 'react';
import { useFieldValidation } from './field-validation.hook';
import { InputFieldSpec, ValueAlias } from 'input-spec';

const UserForm: React.FC = () => {
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [countries, setCountries] = useState<ValueAlias[]>([]);
  const { validateField, getFieldValues } = useFieldValidation();

  const emailFieldSpec: InputFieldSpec = {
    displayName: 'Email',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true,
    constraints: [
      {
        name: 'email',
        format: 'email',
        errorMessage: 'Please enter a valid email address'
      }
    ]
  };

  const handleEmailBlur = async () => {
    const result = await validateField(emailFieldSpec, email);
    setEmailError(result.isValid ? '' : result.errors[0]?.message || 'Invalid email');
  };

  useEffect(() => {
    const loadCountries = async () => {
      const endpoint = {
        uri: '/api/countries',
        responseMapping: {
          dataField: 'data',
          hasNextField: 'hasNext'
        }
      };

      const result = await getFieldValues(endpoint);
      setCountries(result.values);
    };

    loadCountries();
  }, [getFieldValues]);

  return (
    <form>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        onBlur={handleEmailBlur}
        className={emailError ? 'error' : ''}
      />
      {emailError && <div className="error-message">{emailError}</div>}
      
      <select>
        {countries.map((country) => (
          <option key={country.value} value={country.value}>
            {country.label}
          </option>
        ))}
      </select>
    </form>
  );
};

export default UserForm;
```

## Vue.js Integration

### With Custom HTTP Client

```typescript
// plugins/field-validation.ts
import { App } from 'vue';
import { 
  FieldValidator, 
  ValuesResolver, 
  ConfigurableFetchHttpClient,
  MemoryCacheProvider,
  ClientOptions,
  RequestInterceptor,
  ErrorHandler
} from 'input-spec';

// ✅ Create HTTP client with custom interceptors
const httpClient = new ConfigurableFetchHttpClient(
  {
    headers: {
      'Content-Type': 'application/json',
    },
  },
  {
    defaultHeaders: {
      'X-App-Version': '1.0.0',
    },
    timeout: 15000,
    interceptors: [
      // Add authentication token
      async (url, options) => {
        const token = localStorage.getItem('authToken');
        if (token) {
          options.headers = {
            ...options.headers,
            Authorization: `Bearer ${token}`,
          };
        }
        return options;
      },
    ],
    errorHandlers: [
      // Global error handling
      async (error) => {
        if (error.status === 401) {
          window.location.href = '/login';
        }
        console.error('HTTP Error:', error);
      },
    ],
  }
);

const cacheProvider = new MemoryCacheProvider();
const validator = new FieldValidator();
const valuesResolver = new ValuesResolver(httpClient, cacheProvider);

export default {
  install(app: App) {
    app.provide('fieldValidator', validator);
    app.provide('valuesResolver', valuesResolver);
  },
};
```

```vue
<!-- UserForm.vue -->
<template>
  <form @submit.prevent="handleSubmit">
    <input
      v-model="email"
      type="email"
      @blur="validateEmail"
      :class="{ error: emailError }"
    />
    <div v-if="emailError" class="error-message">{{ emailError }}</div>
    
    <select v-model="country" @focus="loadCountries">
      <option v-for="country in countries" :key="country.value" :value="country.value">
        {{ country.label }}
      </option>
    </select>
  </form>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue';
import type { FieldValidator, ValuesResolver, InputFieldSpec, ValueAlias } from 'input-spec';

const fieldValidator = inject<FieldValidator>('fieldValidator')!;
const valuesResolver = inject<ValuesResolver>('valuesResolver')!;

const email = ref('');
const emailError = ref('');
const country = ref('');
const countries = ref<ValueAlias[]>([]);

const emailFieldSpec: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [
    {
      name: 'email',
      format: 'email',
      errorMessage: 'Please enter a valid email address'
    }
  ]
};

const validateEmail = async () => {
  const result = await fieldValidator.validate(emailFieldSpec, email.value);
  emailError.value = result.isValid ? '' : result.errors[0]?.message || 'Invalid email';
};

const loadCountries = async () => {
  const endpoint = {
    uri: '/api/countries',
    responseMapping: {
      dataField: 'data',
      hasNextField: 'hasNext'
    }
  };

  const result = await valuesResolver.resolveValues(endpoint);
  countries.value = result.values;
};
</script>
```

## Vanilla JavaScript Integration

### For frameworks without specific HTTP clients

```javascript
// field-validation.js
import { 
  FieldValidator, 
  ValuesResolver, 
  HttpClientFactory,
  MemoryCacheProvider 
} from 'input-spec';

class FieldValidationManager {
  constructor() {
    // ✅ Auto-detect or use fetch with custom configuration
    this.httpClient = HttpClientFactory.createFetchAdapter({
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
      },
    });

    this.cacheProvider = new MemoryCacheProvider();
    this.validator = new FieldValidator();
    this.valuesResolver = new ValuesResolver(this.httpClient, this.cacheProvider);

    // Add global error handling
    this.httpClient.addErrorHandler((error) => {
      console.error('Validation request failed:', error);
      if (error.status === 401) {
        this.handleUnauthorized();
      }
    });

    // Add authentication interceptor
    this.httpClient.addInterceptor((url, options) => {
      const token = this.getAuthToken();
      if (token) {
        options.headers = {
          ...options.headers,
          Authorization: `Bearer ${token}`,
        };
      }
      return options;
    });
  }

  getAuthToken() {
    return localStorage.getItem('authToken');
  }

  handleUnauthorized() {
    // Redirect to login or refresh token
    window.location.href = '/login';
  }

  async validateField(fieldSpec, value) {
    return this.validator.validate(fieldSpec, value);
  }

  async getFieldValues(endpoint, options) {
    return this.valuesResolver.resolveValues(endpoint, options);
  }
}

// Export singleton instance
export const fieldValidation = new FieldValidationManager();
```

## Key Benefits of This Approach

### ✅ Framework Compatibility
- **Angular**: Uses Angular's HttpClient with full interceptor support
- **React**: Works with Axios configurations and custom setups
- **Vue**: Integrates with Vue's dependency injection system
- **Vanilla**: Provides configurable HTTP client with interceptors

### ✅ Preserves Existing Infrastructure
- **Authentication**: Your existing auth interceptors continue to work
- **Error Handling**: Global error handlers remain functional
- **Caching**: Can integrate with existing cache systems
- **Monitoring**: Request/response logging continues to work

### ✅ Zero Breaking Changes
- Existing HTTP configurations are preserved
- No need to modify interceptor logic
- Gradual adoption possible
- Framework-agnostic core library

### ✅ Enhanced Developer Experience
- Type-safe integration with TypeScript
- Consistent API across all frameworks
- Easy testing with dependency injection
- Configurable error handling and retries