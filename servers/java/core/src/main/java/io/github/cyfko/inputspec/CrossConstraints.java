package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Container annotation for repeatable {@link CrossConstraint} declarations.
 *
 * <p>This annotation is automatically applied by the compiler when multiple
 * {@code @CrossConstraint} annotations are placed on the same class.
 * It should never be used directly by developers.</p>
 *
 * @see CrossConstraint
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface CrossConstraints{
    CrossConstraint[] value();
}
