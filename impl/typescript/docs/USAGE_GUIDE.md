# Usage Guide: From Zero to Hero

## 🚀 Getting Started Journey

### Step 1: Basic Field Validation

Let's start with a simple email field validation - the most common use case.

```typescript
import { FieldValidator, InputFieldSpec } from 'input-spec';

// 1. Define your field specification
const emailFieldSpec: InputFieldSpec = {
  displayName: 'Email Address',
  description: 'Enter your email address',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true, // Required field now at top level
  constraints: [
    {
      name: 'email',
      pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
      errorMessage: 'Please enter a valid email address'
    }
  ]
};

// 2. Create validator instance
const validator = new FieldValidator();

// 3. Validate user input
async function validateEmail(userInput: string) {
  // The validator now automatically checks field-level required first
  const emailResult = await validator.validate(
    emailFieldSpec, 
    userInput, 
    'email'
  );
  
  return emailResult;
}

// Usage example
const result = await validateEmail('user@example.com');
console.log(result.isValid); // true

// Test required validation
const emptyResult = await validateEmail('');
console.log(emptyResult.isValid); // false - triggers field-level required check
console.log(emptyResult.errors[0].message); // "This field is required"
```

### Step 2: Dynamic Values with Remote API

Now let's add dynamic values from a REST API - perfect for country/city selectors.

```typescript
import { 
  ValuesResolver, 
  FetchHttpClient, 
  MemoryCacheProvider,
  ValuesEndpoint 
} from 'input-spec';

// 1. Configure your HTTP client and cache
const httpClient = new FetchHttpClient(10000); // 10s timeout
const cache = new MemoryCacheProvider();
const resolver = new ValuesResolver(httpClient, cache);

// 2. Define your endpoint configuration
const countriesEndpoint: ValuesEndpoint = {
  uri: 'https://restcountries.com/v3.1/all',
  method: 'GET',
  debounceMs: 300,           // Wait 300ms before search
  minSearchLength: 2,        // Minimum 2 characters to search
  cacheStrategy: 'SHORT_TERM', // Cache for 5 minutes
  paginationStrategy: 'PAGE_NUMBER',
  responseMapping: {
    dataField: '',           // Response is direct array
    hasNextField: '',        // No pagination info
    totalField: '',
    pageField: ''
  },
  requestParams: {
    searchParam: 'name',     // Search parameter name
    limitParam: 'limit',
    defaultLimit: 20
  }
};

// 3. Resolve values with search and pagination
async function searchCountries(query: string, page = 1) {
  try {
    const result = await resolver.resolveValues(countriesEndpoint, {
      search: query,
      page: page,
      limit: 10
    });

    // Transform API response to our format
    const countries = result.values.map((country: any) => ({
      value: country.cca2,           // Country code
      label: country.name.common     // Country name
    }));

    return {
      values: countries,
      hasNext: result.hasNext,
      total: result.total
    };
  } catch (error) {
    console.error('Failed to fetch countries:', error);
    return { values: [], hasNext: false, total: 0 };
  }
}

// Usage example
const countries = await searchCountries('fra', 1);
console.log(countries.values); 
// [{ value: 'FR', label: 'France' }]
```

### Step 3: Complex Form with Multiple Constraints

Let's build a user registration form with multiple field types and validation rules.

```typescript
// 1. Password field with strength validation
const passwordFieldSpec: InputFieldSpec = {
  displayName: 'Password',
  description: 'Create a secure password',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true, // Password is required
  constraints: [
    {
      name: 'length',
      min: 8,
      max: 128,
      errorMessage: 'Password must be 8-128 characters'
    },
    {
      name: 'strength',
      pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])',
      errorMessage: 'Password must contain uppercase, lowercase, number and special character'
    }
  ]
};

// 2. Age field with numeric validation
const ageFieldSpec: InputFieldSpec = {
  displayName: 'Age',
  dataType: 'NUMBER',
  expectMultipleValues: false,
  required: true, // Age is required
  constraints: [
    {
      name: 'range',
      min: 18,
      max: 120,
      errorMessage: 'Age must be between 18 and 120'
    }
  ]
};

// 3. Skills field with multiple values
const skillsFieldSpec: InputFieldSpec = {
  displayName: 'Skills',
  description: 'Select your skills',
  dataType: 'STRING',
  expectMultipleValues: true,
  required: false, // Skills are optional
  constraints: [
    {
      name: 'arraySize',
      min: 1,
      max: 10,
      errorMessage: 'Select 1-10 skills'
    },
    {
      name: 'skillFormat',
      enumValues: [
        { value: 'javascript', label: 'JavaScript' },
        { value: 'typescript', label: 'TypeScript' },
        { value: 'react', label: 'React' },
        { value: 'node', label: 'Node.js' },
        { value: 'python', label: 'Python' }
      ]
    }
  ]
};

// 4. Form validation orchestrator
class UserRegistrationForm {
  private validator = new FieldValidator();

  async validateForm(formData: {
    email: string;
    password: string;
    age: number;
    skills: string[];
  }) {
    const errors: Record<string, string[]> = {};

    // Validate email
    const emailResult = await this.validator.validate(
      emailFieldSpec, formData.email, 'email'
    );
    if (!emailResult.isValid) {
      errors.email = emailResult.errors.map(e => e.message);
    }

    // Validate password
    const passwordResults = await Promise.all([
      this.validator.validate(passwordFieldSpec, formData.password, 'required'),
      this.validator.validate(passwordFieldSpec, formData.password, 'length'),
      this.validator.validate(passwordFieldSpec, formData.password, 'strength')
    ]);

    const passwordErrors = passwordResults
      .filter(result => !result.isValid)
      .flatMap(result => result.errors.map(e => e.message));
    
    if (passwordErrors.length > 0) {
      errors.password = passwordErrors;
    }

    // Validate age
    const ageResult = await this.validator.validate(
      ageFieldSpec, formData.age, 'range'
    );
    if (!ageResult.isValid) {
      errors.age = ageResult.errors.map(e => e.message);
    }

    // Validate skills
    const skillsResults = await Promise.all([
      this.validator.validate(skillsFieldSpec, formData.skills, 'arraySize'),
      this.validator.validate(skillsFieldSpec, formData.skills, 'skillFormat')
    ]);

    const skillsErrors = skillsResults
      .filter(result => !result.isValid)
      .flatMap(result => result.errors.map(e => e.message));
    
    if (skillsErrors.length > 0) {
      errors.skills = skillsErrors;
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors
    };
  }
}

// Usage example
const form = new UserRegistrationForm();

const validationResult = await form.validateForm({
  email: 'user@example.com',
  password: 'SecurePass123!',
  age: 25,
  skills: ['javascript', 'typescript', 'react']
});

console.log(validationResult.isValid); // true
```

### Step 4: Integration with React Form

Here's how to integrate with a real React form:

```typescript
import React, { useState, useCallback } from 'react';
import { FieldValidator, ValuesResolver } from 'input-spec';

const UserForm: React.FC = () => {
  const [formData, setFormData] = useState({
    email: '',
    country: '',
    skills: []
  });
  const [errors, setErrors] = useState<Record<string, string[]>>({});
  const [countries, setCountries] = useState([]);

  const validator = new FieldValidator();
  const resolver = new ValuesResolver(httpClient, cache);

  // Real-time validation
  const validateField = useCallback(async (fieldName: string, value: any) => {
    let result;
    
    switch (fieldName) {
      case 'email':
        result = await validator.validate(emailFieldSpec, value, 'email');
        break;
      case 'skills':
        result = await validator.validate(skillsFieldSpec, value, 'arraySize');
        break;
      default:
        return;
    }

    setErrors(prev => ({
      ...prev,
      [fieldName]: result.isValid ? [] : result.errors.map(e => e.message)
    }));
  }, [validator]);

  // Dynamic country search
  const searchCountries = useCallback(async (query: string) => {
    if (query.length < 2) return;
    
    const result = await resolver.resolveValues(countriesEndpoint, {
      search: query
    });
    
    setCountries(result.values);
  }, [resolver]);

  return (
    <form>
      {/* Email field with validation */}
      <div>
        <input
          type="email"
          value={formData.email}
          onChange={e => {
            setFormData(prev => ({ ...prev, email: e.target.value }));
            validateField('email', e.target.value);
          }}
          placeholder="Enter your email"
        />
        {errors.email?.map(error => (
          <div key={error} className="error">{error}</div>
        ))}
      </div>

      {/* Country selector with dynamic search */}
      <div>
        <input
          type="text"
          placeholder="Search countries..."
          onChange={e => searchCountries(e.target.value)}
        />
        <select 
          value={formData.country}
          onChange={e => setFormData(prev => ({ ...prev, country: e.target.value }))}
        >
          {countries.map(country => (
            <option key={country.value} value={country.value}>
              {country.label}
            </option>
          ))}
        </select>
      </div>
    </form>
  );
};
```

### Step 5: Integration with Angular Reactive Forms

Here's a complete Angular integration with Reactive Forms:

```typescript
// user-form.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, switchMap } from 'rxjs';
import { 
  FieldValidator, 
  ValuesResolver, 
  FetchHttpClient, 
  MemoryCacheProvider,
  InputFieldSpec 
} from 'input-spec';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss']
})
export class UserFormComponent implements OnInit, OnDestroy {
  userForm: FormGroup;
  countries: any[] = [];
  skills: any[] = [
    { value: 'javascript', label: 'JavaScript' },
    { value: 'typescript', label: 'TypeScript' },
    { value: 'angular', label: 'Angular' },
    { value: 'react', label: 'React' },
    { value: 'vue', label: 'Vue.js' }
  ];

  private destroy$ = new Subject<void>();
  private validator = new FieldValidator();
  private resolver: ValuesResolver;

  // Field specifications
  private emailFieldSpec: InputFieldSpec = {
    displayName: 'Email',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true, // Required at field level
    constraints: [
      {
        name: 'email',
        pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
        errorMessage: 'Please enter a valid email address'
      }
    ]
  };

  private passwordFieldSpec: InputFieldSpec = {
    displayName: 'Password',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true, // Required at field level
    constraints: [
      {
        name: 'length',
        min: 8,
        max: 128,
        errorMessage: 'Password must be 8-128 characters long'
      },
      {
        name: 'strength',
        pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])',
        errorMessage: 'Password must contain uppercase, lowercase, number and special character'
      }
    ]
  };

  private skillsFieldSpec: InputFieldSpec = {
    displayName: 'Skills',
    dataType: 'STRING',
    expectMultipleValues: true,
    required: false, // Optional field
    constraints: [
      {
        name: 'arraySize',
        min: 1,
        max: 5,
        errorMessage: 'Please select 1-5 skills'
      }
    ]
  };

  constructor(private fb: FormBuilder) {
    // Initialize resolver with zero-dependency implementations
    const httpClient = new FetchHttpClient();
    const cache = new MemoryCacheProvider();
    this.resolver = new ValuesResolver(httpClient, cache);
  }

  ngOnInit(): void {
    this.initializeForm();
    this.setupValidation();
    this.setupCountrySearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.userForm = this.fb.group({
      email: ['', [Validators.required]],
      password: ['', [Validators.required]],
      country: ['', [Validators.required]],
      countrySearch: [''],
      skills: this.fb.array([])
    });
  }

  private setupValidation(): void {
    // Real-time email validation
    this.userForm.get('email')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(async (email: string) => {
        if (email) {
          await this.validateField('email', email);
        }
      });

    // Real-time password validation
    this.userForm.get('password')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(async (password: string) => {
        if (password) {
          await this.validateField('password', password);
        }
      });

    // Skills validation
    this.userForm.get('skills')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(async (skills: string[]) => {
        if (skills && skills.length > 0) {
          await this.validateField('skills', skills);
        }
      });
  }

  private setupCountrySearch(): void {
    this.userForm.get('countrySearch')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => this.searchCountries(query || '')),
        takeUntil(this.destroy$)
      )
      .subscribe(countries => {
        this.countries = countries;
      });
  }

  private async validateField(fieldName: string, value: any): Promise<void> {
    let result;
    let fieldSpec: InputFieldSpec;
    let constraintName: string;

    switch (fieldName) {
      case 'email':
        fieldSpec = this.emailFieldSpec;
        constraintName = 'email';
        break;
      case 'password':
        fieldSpec = this.passwordFieldSpec;
        constraintName = 'strength';
        break;
      case 'skills':
        fieldSpec = this.skillsFieldSpec;
        constraintName = 'arraySize';
        break;
      default:
        return;
    }

    result = await this.validator.validate(fieldSpec, value, constraintName);

    const control = this.userForm.get(fieldName);
    if (control) {
      if (result.isValid) {
        control.setErrors(null);
      } else {
        const errors = result.errors.reduce((acc, error) => {
          acc[error.constraintName] = error.message;
          return acc;
        }, {} as any);
        control.setErrors(errors);
      }
    }
  }

  private async searchCountries(query: string): Promise<any[]> {
    if (query.length < 2) {
      return [];
    }

    try {
      // Mock endpoint for demo - replace with real API
      const mockCountries = [
        { value: 'FR', label: 'France' },
        { value: 'DE', label: 'Germany' },
        { value: 'IT', label: 'Italy' },
        { value: 'ES', label: 'Spain' },
        { value: 'GB', label: 'United Kingdom' },
        { value: 'US', label: 'United States' }
      ];

      return mockCountries.filter(country =>
        country.label.toLowerCase().includes(query.toLowerCase())
      );
    } catch (error) {
      console.error('Error searching countries:', error);
      return [];
    }
  }

  onSkillChange(skillValue: string, checked: boolean): void {
    const skillsArray = this.userForm.get('skills') as FormControl;
    let currentSkills = skillsArray.value || [];

    if (checked) {
      if (!currentSkills.includes(skillValue)) {
        currentSkills = [...currentSkills, skillValue];
      }
    } else {
      currentSkills = currentSkills.filter((skill: string) => skill !== skillValue);
    }

    skillsArray.setValue(currentSkills);
  }

  async onSubmit(): Promise<void> {
    if (this.userForm.valid) {
      // Perform final validation before submission
      const formValue = this.userForm.value;
      
      const validations = await Promise.all([
        this.validator.validate(this.emailFieldSpec, formValue.email, 'email'),
        this.validator.validate(this.passwordFieldSpec, formValue.password, 'strength'),
        this.validator.validate(this.skillsFieldSpec, formValue.skills, 'arraySize')
      ]);

      const hasErrors = validations.some(result => !result.isValid);
      
      if (!hasErrors) {
        console.log('Form submitted successfully:', formValue);
        // Submit to backend
      } else {
        console.log('Form has validation errors');
      }
    } else {
      console.log('Form is invalid');
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.userForm.controls).forEach(key => {
      const control = this.userForm.get(key);
      control?.markAsTouched();
    });
  }

  // Helper methods for template
  hasError(fieldName: string, errorType?: string): boolean {
    const control = this.userForm.get(fieldName);
    if (!control) return false;

    if (errorType) {
      return control.hasError(errorType) && (control.dirty || control.touched);
    }
    return control.invalid && (control.dirty || control.touched);
  }

  getErrorMessage(fieldName: string): string {
    const control = this.userForm.get(fieldName);
    if (!control || !control.errors) return '';

    const errors = control.errors;
    const errorKey = Object.keys(errors)[0];
    return errors[errorKey];
  }

  isSkillSelected(skillValue: string): boolean {
    const skills = this.userForm.get('skills')?.value || [];
    return skills.includes(skillValue);
  }
}
```

```html
<!-- user-form.component.html -->
<div class="user-form-container">
  <h2>User Registration</h2>
  
  <form [formGroup]="userForm" (ngSubmit)="onSubmit()" novalidate>
    
    <!-- Email Field -->
    <div class="form-group">
      <label for="email">Email Address *</label>
      <input
        id="email"
        type="email"
        formControlName="email"
        class="form-control"
        [class.is-invalid]="hasError('email')"
        placeholder="Enter your email address"
      />
      <div *ngIf="hasError('email')" class="invalid-feedback">
        {{ getErrorMessage('email') }}
      </div>
    </div>

    <!-- Password Field -->
    <div class="form-group">
      <label for="password">Password *</label>
      <input
        id="password"
        type="password"
        formControlName="password"
        class="form-control"
        [class.is-invalid]="hasError('password')"
        placeholder="Create a secure password"
      />
      <div *ngIf="hasError('password')" class="invalid-feedback">
        {{ getErrorMessage('password') }}
      </div>
      <small class="form-text text-muted">
        Password must contain uppercase, lowercase, number and special character
      </small>
    </div>

    <!-- Country Search and Selection -->
    <div class="form-group">
      <label for="countrySearch">Country *</label>
      <div class="country-search">
        <input
          id="countrySearch"
          type="text"
          formControlName="countrySearch"
          class="form-control"
          placeholder="Search for a country..."
        />
        <select
          formControlName="country"
          class="form-control mt-2"
          [class.is-invalid]="hasError('country')"
        >
          <option value="">Select a country</option>
          <option 
            *ngFor="let country of countries" 
            [value]="country.value"
          >
            {{ country.label }}
          </option>
        </select>
      </div>
      <div *ngIf="hasError('country')" class="invalid-feedback">
        Please select a country
      </div>
    </div>

    <!-- Skills Selection -->
    <div class="form-group">
      <label>Skills (select 1-5) *</label>
      <div class="skills-container">
        <div 
          *ngFor="let skill of skills" 
          class="form-check"
        >
          <input
            [id]="'skill-' + skill.value"
            type="checkbox"
            class="form-check-input"
            [checked]="isSkillSelected(skill.value)"
            (change)="onSkillChange(skill.value, $event.target.checked)"
          />
          <label 
            [for]="'skill-' + skill.value" 
            class="form-check-label"
          >
            {{ skill.label }}
          </label>
        </div>
      </div>
      <div *ngIf="hasError('skills')" class="invalid-feedback d-block">
        {{ getErrorMessage('skills') }}
      </div>
    </div>

    <!-- Submit Button -->
    <button 
      type="submit" 
      class="btn btn-primary"
      [disabled]="userForm.invalid"
    >
      Register
    </button>
    
    <!-- Form Debug (remove in production) -->
    <div class="mt-4" *ngIf="false">
      <h5>Form Debug:</h5>
      <pre>{{ userForm.value | json }}</pre>
      <p>Form Valid: {{ userForm.valid }}</p>
    </div>
    
  </form>
</div>
```

```scss
/* user-form.component.scss */
.user-form-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem;
  
  .form-group {
    margin-bottom: 1.5rem;
    
    label {
      font-weight: 600;
      margin-bottom: 0.5rem;
      display: block;
    }
    
    .form-control {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
      
      &:focus {
        outline: none;
        border-color: #007bff;
        box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
      }
      
      &.is-invalid {
        border-color: #dc3545;
      }
    }
    
    .invalid-feedback {
      color: #dc3545;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
    
    .form-text {
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
  }
  
  .country-search {
    .form-control + .form-control {
      margin-top: 0.5rem;
    }
  }
  
  .skills-container {
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 1rem;
    
    .form-check {
      margin-bottom: 0.5rem;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .form-check-input {
        margin-right: 0.5rem;
      }
      
      .form-check-label {
        font-weight: normal;
        cursor: pointer;
      }
    }
  }
  
  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    
    &.btn-primary {
      background-color: #007bff;
      color: white;
      
      &:hover:not(:disabled) {
        background-color: #0056b3;
      }
      
      &:disabled {
        background-color: #6c757d;
        cursor: not-allowed;
      }
    }
  }
}
```

```typescript
// app.module.ts - Don't forget to import ReactiveFormsModule
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { UserFormComponent } from './user-form/user-form.component';

@NgModule({
  declarations: [
    AppComponent,
    UserFormComponent
  ],
  imports: [
    BrowserModule,
    ReactiveFormsModule  // Required for reactive forms
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

## 🎭 Common Patterns & Best Practices

### Pattern 1: Validation Chain
```typescript
// Chain multiple validations for complex business rules
const validationChain = [
  { constraint: 'required', spec: fieldSpec },
  { constraint: 'format', spec: fieldSpec },
  { constraint: 'businessRule', spec: fieldSpec }
];

for (const { constraint, spec } of validationChain) {
  const result = await validator.validate(spec, value, constraint);
  if (!result.isValid) {
    return result; // Stop on first error
  }
}
```

### Pattern 2: Conditional Validation
```typescript
// Validate based on other field values
if (formData.accountType === 'business') {
  // Validate business-specific fields
  const taxIdResult = await validator.validate(taxIdSpec, formData.taxId, 'required');
}
```

### Pattern 3: Async Validation with Debouncing
```typescript
// Use built-in debouncing for performance
const debouncedValidation = useMemo(() => 
  debounce(async (value: string) => {
    const result = await resolver.resolveValues(endpoint, { search: value });
    setOptions(result.values);
  }, 300), 
  [resolver]
);
```

This guide takes users from basic validation to complex real-world scenarios! 🚀