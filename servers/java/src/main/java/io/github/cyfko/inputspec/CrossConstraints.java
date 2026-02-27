package io.github.cyfko.inputspec;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface CrossConstraints{
    CrossConstraint[] value();
}
