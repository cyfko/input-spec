package io.github.cyfko.inputspec.validation;

/**
 * Options influencing validation behavior.
 * Designed for forward extensibility without breaking method signatures.
 */
public final class ValidationOptions {
    private final boolean shortCircuit;

    private ValidationOptions(Builder b) {
        this.shortCircuit = b.shortCircuit;
    }

    public boolean isShortCircuit() { return shortCircuit; }

    public static Builder builder() { return new Builder(); }

    public static ValidationOptions shortCircuit() {
        return builder().shortCircuit(true).build();
    }

    public static final class Builder {
        private boolean shortCircuit;
        public Builder shortCircuit(boolean shortCircuit) { this.shortCircuit = shortCircuit; return this; }
        public ValidationOptions build() { return new ValidationOptions(this); }
    }
}
