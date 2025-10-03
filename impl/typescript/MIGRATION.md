# Migration Guide: v1.x to v2.0

This guide helps you migrate from the old constraint structure to the new array-based structure with enhanced API design.

## 🎯 Overview of Changes

### Key Changes in v2.0

1. **Required field moved to top-level**: `required: boolean` is now a direct property of `InputFieldSpec`
2. **Constraints as ordered array**: `constraints` changed from `Record<string, ConstraintDescriptor>` to `ConstraintDescriptor[]`
3. **Named constraints**: Each constraint now has a `name: string` property
4. **Ordered execution**: Constraints execute in array order for predictable behavior

## 📋 Migration Steps

### Step 1: Update InputFieldSpec Structure

**Before (v1.x):**
```typescript
const fieldSpec: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  expectMultipleValues: false,
  constraints: {
    email: {
      required: true,
      pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
      errorMessage: 'Please enter a valid email'
    }
  }
};
```

**After (v2.0):**
```typescript
const fieldSpec: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,  // ✨ Moved to top-level
  constraints: [   // ✨ Now an array
    {
      name: 'email',  // ✨ Added name property
      pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
      errorMessage: 'Please enter a valid email'
      // ✨ No more 'required' property here
    }
  ]
};
```

### Step 2: Update Multiple Constraints

**Before (v1.x):**
```typescript
const passwordSpec: InputFieldSpec = {
  displayName: 'Password',
  dataType: 'STRING',
  expectMultipleValues: false,
  constraints: {
    length: {
      required: false,
      min: 8,
      max: 50,
      errorMessage: 'Password must be 8-50 characters'
    },
    strength: {
      required: false,
      pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)',
      errorMessage: 'Must contain lowercase, uppercase, and digit'
    }
  }
};
```

**After (v2.0):**
```typescript
const passwordSpec: InputFieldSpec = {
  displayName: 'Password',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,  // ✨ Set field-level requirement
  constraints: [   // ✨ Ordered array execution
    {
      name: 'length',    // ✨ Executes first
      min: 8,
      max: 50,
      errorMessage: 'Password must be 8-50 characters'
    },
    {
      name: 'strength',  // ✨ Executes second
      pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)',
      errorMessage: 'Must contain lowercase, uppercase, and digit'
    }
  ]
};
```

### Step 3: Update Validation Calls

The validation API remains the same, but you benefit from ordered execution:

```typescript
const validator = new FieldValidator();

// Validate specific constraint (unchanged)
const result = await validator.validate(passwordSpec, 'weak', 'length');

// Validate all constraints (now executes in array order)
const allResult = await validator.validate(passwordSpec, 'StrongPass123!');
```

## 🔧 Automated Migration Script

Here's a script to help automate the migration:

```typescript
function migrateInputFieldSpec(oldSpec: any): InputFieldSpec {
  const newSpec: InputFieldSpec = {
    displayName: oldSpec.displayName,
    description: oldSpec.description,
    dataType: oldSpec.dataType,
    expectMultipleValues: oldSpec.expectMultipleValues,
    required: false, // Default to false
    constraints: []
  };

  // Extract required from any constraint and set at field level
  for (const [name, constraint] of Object.entries(oldSpec.constraints)) {
    if ((constraint as any).required) {
      newSpec.required = true;
    }
    
    // Convert to new constraint structure
    const newConstraint: ConstraintDescriptor = {
      name,
      ...(constraint as any)
    };
    
    // Remove the old required property
    delete (newConstraint as any).required;
    
    newSpec.constraints.push(newConstraint);
  }

  return newSpec;
}

// Usage
const oldSpec = { /* your old spec */ };
const newSpec = migrateInputFieldSpec(oldSpec);
```

## 📝 Common Migration Patterns

### Pattern 1: Simple Required Field

**Before:**
```typescript
constraints: {
  value: { required: true }
}
```

**After:**
```typescript
required: true,
constraints: [
  { name: 'value' }
]
```

### Pattern 2: Optional Field with Validation

**Before:**
```typescript
constraints: {
  email: { 
    required: false, 
    pattern: '...' 
  }
}
```

**After:**
```typescript
required: false,
constraints: [
  { 
    name: 'email', 
    pattern: '...' 
  }
]
```

### Pattern 3: Multiple Constraints

**Before:**
```typescript
constraints: {
  length: { required: false, min: 3, max: 20 },
  format: { required: false, pattern: '^[a-zA-Z]+$' }
}
```

**After:**
```typescript
required: false,
constraints: [
  { name: 'length', min: 3, max: 20 },
  { name: 'format', pattern: '^[a-zA-Z]+$' }
]
```

### Pattern 4: Array Fields

**Before:**
```typescript
expectMultipleValues: true,
constraints: {
  arraySize: { 
    required: false, 
    min: 1, 
    max: 5 
  }
}
```

**After:**
```typescript
expectMultipleValues: true,
required: false,
constraints: [
  { 
    name: 'arraySize', 
    min: 1, 
    max: 5 
  }
]
```

## ✅ Validation Checklist

After migration, verify:

- [ ] Field-level `required` is set correctly
- [ ] All constraints are in the `constraints` array
- [ ] Each constraint has a unique `name` property
- [ ] No constraint has a `required` property
- [ ] Constraint execution order is logical
- [ ] Tests pass with new structure
- [ ] Validation behavior is preserved

## 🎯 Benefits of v2.0 Structure

### 1. **Clearer API**
```typescript
// Required status is immediately visible
const spec = {
  required: true,  // Clear and obvious
  constraints: [...]
};
```

### 2. **Ordered Execution**
```typescript
constraints: [
  { name: 'length', ... },    // Runs first
  { name: 'format', ... },    // Runs second  
  { name: 'business', ... }   // Runs third
]
```

### 3. **Better Error Handling**
```typescript
// Errors now include constraint name and position
{
  constraintName: 'length',
  message: 'Too short',
  // More context available
}
```

### 4. **Enhanced Debugging**
```typescript
// You can validate constraints individually in order
await validator.validate(spec, value, 'length');
await validator.validate(spec, value, 'format');
```

## 🚀 Next Steps

1. **Update your specs**: Use the migration patterns above
2. **Test thoroughly**: Ensure validation behavior is preserved
3. **Update documentation**: Reflect the new structure in your docs
4. **Train your team**: Share this migration guide with your team
5. **Enjoy the benefits**: Leverage ordered execution and cleaner API

## 💡 Tips

- **Start small**: Migrate one field spec at a time
- **Test incrementally**: Validate each migration step
- **Use TypeScript**: Let the compiler guide your migration
- **Check examples**: Look at the updated examples in `/examples/`
- **Ask for help**: Open an issue if you need assistance

## 📚 Additional Resources

- [Updated Examples](./examples/)
- [API Reference](./docs/API.md)
- [USAGE_GUIDE.md](./docs/USAGE_GUIDE.md)
- [PROTOCOL_SPECIFICATION.md](../../PROTOCOL_SPECIFICATION.md)