/**
 * Complete Form Example
 * 
 * This example shows how to build a complete user registration form
 * combining validation and dynamic values in a real-world scenario.
 */

import { 
  FieldValidator, 
  ValuesResolver,
  InputFieldSpec,
  ValidationResult,
  FetchHttpClient,
  MemoryCacheProvider 
} from '../src';

import { MockHttpClient, countriesEndpoint } from './dynamic-values';

// Form field specifications
const formFields = {
  email: {
    displayName: 'Email Address',
    dataType: 'STRING' as const,
    expectMultipleValues: false,
    required: true,
    constraints: [
      {
        name: 'email',
        pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
        errorMessage: 'Please enter a valid email address'
      }
    ]
  },

  password: {
    displayName: 'Password',
    dataType: 'STRING' as const,
    expectMultipleValues: false,
    required: true,
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
  },

  age: {
    displayName: 'Age',
    dataType: 'NUMBER' as const,
    expectMultipleValues: false,
    required: true,
    constraints: [
      {
        name: 'range',
        min: 18,
        max: 120,
        errorMessage: 'Age must be between 18 and 120'
      }
    ]
  },

  country: {
    displayName: 'Country',
    dataType: 'STRING' as const,
    expectMultipleValues: false,
    required: true,
    constraints: []
  },

  interests: {
    displayName: 'Interests',
    dataType: 'STRING' as const,
    expectMultipleValues: true,
    required: false,
    constraints: [
      {
        name: 'arraySize',
        min: 1,
        max: 5,
        errorMessage: 'Please select 1-5 interests'
      },
      {
        name: 'validInterests',
        enumValues: [
          { value: 'technology', label: 'Technology' },
          { value: 'sports', label: 'Sports' },
          { value: 'music', label: 'Music' },
          { value: 'travel', label: 'Travel' },
          { value: 'cooking', label: 'Cooking' },
          { value: 'reading', label: 'Reading' },
          { value: 'gaming', label: 'Gaming' }
        ]
      }
    ]
  }
};

// Form data type
interface UserFormData {
  email: string;
  password: string;
  age: number;
  country: string;
  interests: string[];
}

// Form validation class
class UserRegistrationForm {
  private validator = new FieldValidator();
  private resolver: ValuesResolver;

  constructor() {
    // Use mock client for demo, replace with real FetchHttpClient in production
    const httpClient = new MockHttpClient() as any;
    const cache = new MemoryCacheProvider();
    this.resolver = new ValuesResolver(httpClient, cache);
  }

  async validateField(
    fieldName: keyof UserFormData, 
    value: any, 
    constraintName?: string
  ): Promise<ValidationResult> {
    const fieldSpec = formFields[fieldName] as InputFieldSpec;
    
    if (!fieldSpec) {
      return {
        isValid: false,
        errors: [{ constraintName: 'field', message: 'Unknown field' }]
      };
    }

    // If no specific constraint, validate all constraints
    if (!constraintName) {
      const constraintNames = fieldSpec.constraints.map(c => c.name);
      const results = await Promise.all(
        constraintNames.map(name => 
          this.validator.validate(fieldSpec, value, name)
        )
      );

      const errors = results
        .filter(result => !result.isValid)
        .flatMap(result => result.errors);

      return {
        isValid: errors.length === 0,
        errors
      };
    }

    return this.validator.validate(fieldSpec, value, constraintName);
  }

  async validateForm(formData: UserFormData): Promise<{
    isValid: boolean;
    errors: Record<string, string[]>;
  }> {
    const errors: Record<string, string[]> = {};

    // Validate each field
    for (const [fieldName, value] of Object.entries(formData)) {
      const result = await this.validateField(
        fieldName as keyof UserFormData, 
        value
      );

      if (!result.isValid) {
        errors[fieldName] = result.errors.map(e => e.message);
      }
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors
    };
  }

  async searchCountries(query: string) {
    try {
      const result = await this.resolver.resolveValues(countriesEndpoint, {
        search: query,
        limit: 10
      });
      return result.values;
    } catch (error) {
      console.error('Failed to search countries:', error);
      return [];
    }
  }

  // Real-time validation as user types
  async validateRealTime(fieldName: keyof UserFormData, value: any) {
    // Only validate non-empty values to avoid showing errors immediately
    if (!value || (Array.isArray(value) && value.length === 0)) {
      return { isValid: true, errors: [] };
    }

    return this.validateField(fieldName, value);
  }
}

// Demo function
async function runCompleteFormExample() {
  console.log('📋 Complete Form Validation Example\n');

  const form = new UserRegistrationForm();

  // Test data scenarios
  const testCases = [
    {
      name: 'Valid User Data',
      data: {
        email: 'john.doe@example.com',
        password: 'SecurePass123!',
        age: 28,
        country: 'US',
        interests: ['technology', 'music', 'travel']
      }
    },
    {
      name: 'Invalid User Data',
      data: {
        email: 'invalid-email',
        password: 'weak',
        age: 15,
        country: '',
        interests: [] as string[]
      }
    },
    {
      name: 'Partially Valid Data',
      data: {
        email: 'user@example.com',
        password: 'NoSpecialChar123',
        age: 25,
        country: 'FR',
        interests: ['tech', 'sports', 'music', 'travel', 'cooking', 'reading'] // Too many
      }
    }
  ];

  for (const testCase of testCases) {
    console.log(`\n--- Testing: ${testCase.name} ---`);
    console.log('Data:', JSON.stringify(testCase.data, null, 2));

    const result = await form.validateForm(testCase.data);
    
    console.log(`\nValidation Result: ${result.isValid ? '✅ Valid' : '❌ Invalid'}`);
    
    if (!result.isValid) {
      console.log('Errors:');
      Object.entries(result.errors).forEach(([field, errors]) => {
        console.log(`  ${field}:`);
        errors.forEach(error => console.log(`    - ${error}`));
      });
    }
  }

  // Demo real-time validation
  console.log('\n--- Real-time Validation Demo ---');
  
  const emailTests = ['u', 'user', 'user@', 'user@example', 'user@example.com'];
  
  for (const email of emailTests) {
    const result = await form.validateRealTime('email', email);
    console.log(`Email "${email}": ${result.isValid ? '✅' : '❌'} ${
      result.errors.length > 0 ? `(${result.errors[0].message})` : ''
    }`);
  }

  // Demo country search
  console.log('\n--- Country Search Demo ---');
  const searchQueries = ['uni', 'unite', 'united states'];
  
  for (const query of searchQueries) {
    const countries = await form.searchCountries(query);
    console.log(`Search "${query}": ${countries.length} results`);
    countries.slice(0, 3).forEach(country => {
      console.log(`  - ${country.label} (${country.value})`);
    });
  }
}

// Simulate form interaction
async function simulateFormInteraction() {
  console.log('\n🎭 Simulating User Form Interaction\n');

  const form = new UserRegistrationForm();
  
  console.log('User starts filling registration form...');
  
  // Step 1: User types email
  console.log('\n1. User types email address...');
  const emailSteps = ['j', 'jo', 'john', 'john@', 'john@ex', 'john@example.com'];
  
  for (const step of emailSteps) {
    const result = await form.validateRealTime('email', step);
    const status = result.isValid ? '✅' : '❌';
    const message = result.errors.length > 0 ? ` - ${result.errors[0].message}` : '';
    console.log(`   "${step}" ${status}${message}`);
  }

  // Step 2: User searches for country
  console.log('\n2. User searches for country...');
  const countrySearch = 'fran';
  const countries = await form.searchCountries(countrySearch);
  console.log(`   Search "${countrySearch}" shows ${countries.length} options:`);
  countries.forEach(country => {
    console.log(`   • ${country.label}`);
  });

  // Step 3: User selects interests
  console.log('\n3. User selects interests...');
  const interestSteps = [
    ['technology'],
    ['technology', 'music'],
    ['technology', 'music', 'travel'],
    ['technology', 'music', 'travel', 'sports', 'cooking', 'reading'] // Too many
  ];

  for (const interests of interestSteps) {
    const result = await form.validateRealTime('interests', interests);
    const status = result.isValid ? '✅' : '❌';
    const message = result.errors.length > 0 ? ` - ${result.errors[0].message}` : '';
    console.log(`   ${interests.length} interests selected ${status}${message}`);
  }

  console.log('\n✨ Form interaction simulation complete!');
}

// Export for use in main demo
export { 
  runCompleteFormExample, 
  simulateFormInteraction,
  UserRegistrationForm,
  formFields 
};

// Run examples if this file is executed directly
async function main() {
  await runCompleteFormExample();
  await simulateFormInteraction();
}

// Auto-run if this is the main module
if (typeof window === 'undefined' && typeof process !== 'undefined') {
  main().catch(console.error);
}