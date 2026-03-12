package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import io.github.cyfko.inputspec.validation.FormValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Scans the Spring ApplicationContext for methods annotated with {@link FormValidator}
 * and registers them as handlers with the {@link FormSpecValidator}.
 *
 * <p>The routing context is determined by the method's return type:</p>
 * <ul>
 *     <li>{@code Optional<String>} → Registers as a CustomCrossConstraintHandler (Phase 2 context)</li>
 *     <li>{@code Map<String, String>} → Registers as a GlobalFormValidatorHandler (Phase 3 context)</li>
 * </ul>
 * <p>In both cases, Jackson is used to map the raw JSON payload map to the method's expected POJO parameter.</p>
 */
public class FormValidatorRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(FormValidatorRegistry.class);

    private final ApplicationContext context;
    private final FormSpecValidator validator;
    private final ObjectMapper mapper;

    public FormValidatorRegistry(ApplicationContext context,
                                 FormSpecValidator validator,
                                 ObjectMapper mapper) {
        this.context = context;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = context.getBeansOfType(Object.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            ReflectionUtils.doWithMethods(targetClass, method -> {
                FormValidator annotation = AnnotationUtils.findAnnotation(method, FormValidator.class);
                if (annotation != null) {
                    validateSignatureAndRegister(bean, method, annotation);
                }
            });
        }
    }

    private void validateSignatureAndRegister(Object bean, Method method, FormValidator annotation) {
        if (method.getParameterCount() != 1) {
            throw new FormValidatorConfigurationException(
                "Method " + method.getName() + " in " + bean.getClass().getName() +
                " annotated with @FormValidator must accept exactly ONE parameter (the FormSpec POJO).");
        }

        Class<?> paramType = method.getParameterTypes()[0];
        Class<?> returnType = method.getReturnType();
        String targetId = annotation.value();

        if (Optional.class.isAssignableFrom(returnType)) {
            // Phase 2: Constraint-level context
            registerConstraintHandler(bean, method, paramType, targetId);

        } else if (Map.class.isAssignableFrom(returnType)) {
            // Phase 3: Global form-level context
            registerGlobalHandler(bean, method, paramType, targetId);

        } else {
            throw new FormValidatorConfigurationException(
                "Method " + method.getName() + " in " + bean.getClass().getName() +
                " annotated with @FormValidator has an unsupported return type. " +
                "Must return Optional<String> (for custom keys) or Map<String, String> (for global validation).");
        }
    }

    private void registerConstraintHandler(Object bean, Method method, Class<?> paramType, String customKey) {
        log.info("DIFSP: Registering @FormValidator (Constraint Context) -> '{}' mapped to {}.{}",
                 customKey, bean.getClass().getSimpleName(), method.getName());

        validator.registerCustomCrossHandler(customKey, (fieldValues, params) -> {
            try {
                // Map the untyped Map<String, Object> into the strongly typed POJO
                Object pojo = mapper.convertValue(fieldValues, paramType);
                // Invoke user's method which returns Optional<String>
                @SuppressWarnings("unchecked")
                Optional<String> result = (Optional<String>) ReflectionUtils.invokeMethod(method, bean, pojo);
                return result;
            } catch (Exception e) {
                log.error("Failed to map or invoke @FormValidator (Constraint Context) method {}.{}",
                          bean.getClass().getSimpleName(), method.getName(), e);
                return Optional.of("Internal server error during constraint validation");
            }
        });
    }

    private void registerGlobalHandler(Object bean, Method method, Class<?> paramType, String formId) {
        log.info("DIFSP: Registering @FormValidator (Global Context) -> '{}' mapped to {}.{}",
                 formId, bean.getClass().getSimpleName(), method.getName());

        validator.registerGlobalFormHandler(formId, fieldValues -> {
            try {
                // Map the untyped Map<String, Object> into the strongly typed POJO
                Object pojo = mapper.convertValue(fieldValues, paramType);
                // Invoke user's method which returns Map<String, String>
                @SuppressWarnings("unchecked")
                Map<String, String> result = (Map<String, String>) ReflectionUtils.invokeMethod(method, bean, pojo);
                return result;
            } catch (Exception e) {
                log.error("Failed to map or invoke @FormValidator (Global Context) method {}.{}",
                          bean.getClass().getSimpleName(), method.getName(), e);
                return Map.of("global", "Internal server error during global validation");
            }
        });
    }

    /**
     * Internal exception for invalid @FormValidator configurations during startup.
     */
    public static class FormValidatorConfigurationException extends RuntimeException {
        public FormValidatorConfigurationException(String message) {
            super(message);
        }
    }
}
