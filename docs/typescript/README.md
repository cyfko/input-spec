# Documentation Index

Welcome to the Dynamic Input Field Specification Protocol v2.0 TypeScript implementation documentation!

## 📚 Documentation Structure

### 🏗️ [Architecture Overview](./ARCHITECTURE.md)
- System architecture diagrams
- Component interaction flows  
- Data flow sequences
- Design patterns explanation

### 📖 [Usage Guide](./USAGE_GUIDE.md)
- Step-by-step learning journey
- From basic validation to complex forms
- Real-world integration examples
- Best practices and patterns

## 🎯 Learning Path

### For Beginners
1. Start with the [Usage Guide](./USAGE_GUIDE.md) - Step 1: Basic Field Validation
2. Try the basic validation example: `npm run examples:basic`
3. Read about the [Architecture](./ARCHITECTURE.md) - Component Interaction

### For Intermediate Users
1. [Usage Guide](./USAGE_GUIDE.md) - Step 2: Dynamic Values with Remote API
2. Run dynamic values example: `npm run examples:dynamic`
3. Study the [Architecture](./ARCHITECTURE.md) - Data Flow Diagram

### For Advanced Users
1. [Usage Guide](./USAGE_GUIDE.md) - Step 3: Complex Form with Multiple Constraints
2. Try the complete form example: `npm run examples:complete`
3. Review the [Architecture](./ARCHITECTURE.md) - System Architecture Diagram

## 🛠️ Quick References

### Common Use Cases
- **Email Validation**: See [Usage Guide - Step 1](./USAGE_GUIDE.md#step-1-basic-field-validation)
- **Dynamic Country Selector**: See [Usage Guide - Step 2](./USAGE_GUIDE.md#step-2-dynamic-values-with-remote-api)
- **Password Strength**: See [Usage Guide - Step 3](./USAGE_GUIDE.md#step-3-complex-form-with-multiple-constraints)
- **Multi-value Fields**: See [Usage Guide - Step 3](./USAGE_GUIDE.md#step-3-complex-form-with-multiple-constraints)

### Integration Examples
- **React Forms**: See [Usage Guide - Step 4](./USAGE_GUIDE.md#step-4-integration-with-react-form)
- **Vue.js**: Similar pattern to React example
- **Angular**: Similar pattern with reactive forms

### Design Patterns
- **Dependency Injection**: [Architecture - Component Interaction](./ARCHITECTURE.md#-component-interaction)
- **Strategy Pattern**: [Architecture - System Architecture](./ARCHITECTURE.md#️-system-architecture-diagram)
- **Template Method**: See validation engine in [Usage Guide](./USAGE_GUIDE.md)

## 🚀 Interactive Examples

All examples are executable and demonstrate real functionality:

```bash
# Run individual examples
npm run examples:basic      # Basic validation concepts
npm run examples:dynamic    # Dynamic values with APIs
npm run examples:complete   # Complete form scenarios

# Run full demonstration
npm run examples:demo       # Everything with performance metrics
```

## 📋 API Reference

### Core Types
- `InputFieldSpec` - Complete field specification
- `ConstraintDescriptor` - Validation rules configuration
- `ValidationResult` - Validation outcome with errors
- `ValuesEndpoint` - Dynamic values API configuration

### Main Classes
- `FieldValidator` - Validation engine
- `ValuesResolver` - Dynamic values orchestrator
- `FetchHttpClient` - HTTP client implementation
- `MemoryCacheProvider` - Caching implementation

### Interfaces
- `HttpClient` - HTTP client abstraction
- `CacheProvider` - Cache provider abstraction

## 🎯 Next Steps

1. **Try the Examples**: Start with `npm run examples:basic`
2. **Read the Guide**: Follow the [Usage Guide](./USAGE_GUIDE.md) learning path
3. **Understand Architecture**: Review [Architecture](./ARCHITECTURE.md) diagrams
4. **Build Something**: Create your own field specifications
5. **Integrate**: Use with your favorite UI framework
6. **Test**: Run `npm test` to see comprehensive test coverage
7. **Build**: Use `npm run build` to create distribution files

Happy coding! 🎉