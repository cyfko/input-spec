/**
 * Basic Validation Example
 * 
 * This example shows how to use the FieldValidator for basic form validation
 * with different data types and constraints.
 */

import { FieldValidator, InputFieldSpec } from '../src';

// Example 1: Email validation
const emailField: InputFieldSpec = {
  displayName: 'Email Address',
  description: 'User email address',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true, // Required field
  constraints: [
    {
      name: 'email',
      pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
      errorMessage: 'Please enter a valid email address'
    }
  ]
};

// Example 2: Age validation
const ageField: InputFieldSpec = {
  displayName: 'Age',
  dataType: 'NUMBER',
  expectMultipleValues: false,
  required: true, // Required field
  constraints: [
    {
      name: 'range',
      min: 18,
      max: 120,
      errorMessage: 'Age must be between 18 and 120'
    }
  ]
};

// Example 3: Multi-value skills
const skillsField: InputFieldSpec = {
  displayName: 'Skills',
  dataType: 'STRING',
  expectMultipleValues: true,
  required: false, // Optional field
  constraints: [
    {
      name: 'arraySize',
      min: 1,
      max: 5,
      errorMessage: 'Select 1-5 skills'
    }
  ]
};

async function runBasicValidationExamples() {
  const validator = new FieldValidator();

  console.log('🔍 Basic Validation Examples\n');

  // Test 1: Valid email
  console.log('1. Testing valid email...');
  const validEmailResult = await validator.validate(
    emailField, 
    'user@example.com', 
    'email'
  );
  console.log(`   Result: ${validEmailResult.isValid ? '✅ Valid' : '❌ Invalid'}`);
  
  // Test 2: Invalid email
  console.log('2. Testing invalid email...');
  const invalidEmailResult = await validator.validate(
    emailField, 
    'invalid-email', 
    'email'
  );
  console.log(`   Result: ${invalidEmailResult.isValid ? '✅ Valid' : '❌ Invalid'}`);
  if (!invalidEmailResult.isValid) {
    console.log(`   Error: ${invalidEmailResult.errors[0].message}`);
  }

  // Test 3: Age validation
  console.log('3. Testing age validation...');
  const ageResult = await validator.validate(ageField, 25, 'range');
  console.log(`   Age 25: ${ageResult.isValid ? '✅ Valid' : '❌ Invalid'}`);

  const invalidAgeResult = await validator.validate(ageField, 150, 'range');
  console.log(`   Age 150: ${invalidAgeResult.isValid ? '✅ Valid' : '❌ Invalid'}`);
  if (!invalidAgeResult.isValid) {
    console.log(`   Error: ${invalidAgeResult.errors[0].message}`);
  }

  // Test 4: Multi-value validation
  console.log('4. Testing skills array...');
  const skillsResult = await validator.validate(
    skillsField, 
    ['JavaScript', 'TypeScript', 'React'], 
    'arraySize'
  );
  console.log(`   3 skills: ${skillsResult.isValid ? '✅ Valid' : '❌ Invalid'}`);

  const tooManySkillsResult = await validator.validate(
    skillsField, 
    ['JS', 'TS', 'React', 'Node', 'Python', 'Java'], 
    'arraySize'
  );
  console.log(`   6 skills: ${tooManySkillsResult.isValid ? '✅ Valid' : '❌ Invalid'}`);
  if (!tooManySkillsResult.isValid) {
    console.log(`   Error: ${tooManySkillsResult.errors[0].message}`);
  }
}

// Run examples if this file is executed directly
if (require.main === module) {
  runBasicValidationExamples().catch(console.error);
}

export { runBasicValidationExamples };