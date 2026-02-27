package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Registry of all {@code @FormHandler} methods in the application.
 *
 * Built exactly once when the Spring context is fully initialized
 * ({@link SmartInitializingSingleton}), before the application accepts traffic.
 *
 * <h3>Startup guarantees — application refuses to start if:</h3>
 * <ol>
 *   <li>A form in FormSpecCache has no @FormHandler  (broken contract)</li>
 *   <li>Two @FormHandlers target the same formId     (ambiguous routing)</li>
 *   <li>A @FormHandler targets an unknown formId     (orphan handler)</li>
 *   <li>A @FormHandler method has wrong signature    (not exactly 1 param + SubmitResponse)</li>
 * </ol>
 *
 * At runtime, {@link #find(String)} is a plain Map lookup — O(1).
 */
public class FormHandlerRegistry implements SmartInitializingSingleton {

    private final ApplicationContext context;
    private final FormSpecCache      cache;
    private final ObjectMapper       mapper;

    private final Map<String, ResolvedHandler> registry = new HashMap<>();

    public FormHandlerRegistry(ApplicationContext context,
                                FormSpecCache cache,
                                ObjectMapper mapper) {
        this.context = context;
        this.cache   = cache;
        this.mapper  = mapper;
    }

    // ─── Startup ─────────────────────────────────────────────────────────────

    @Override
    public void afterSingletonsInstantiated() {
        scanHandlers();
        validateCoverage();
    }

    private void scanHandlers() {
        context.getBeansOfType(Object.class).forEach((beanName, bean) -> {
            // See through CGLIB / JDK proxies
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {
                FormHandler annotation = method.getAnnotation(FormHandler.class);
                if (annotation == null) continue;

                String formId = annotation.value();

                // Duplicate
                if (registry.containsKey(formId)) {
                    ResolvedHandler existing = registry.get(formId);
                    throw new FormHandlerConfigurationException(String.format(
                        "Duplicate @FormHandler for form '%s':%n  → %s#%s%n  → %s#%s%n" +
                        "Only one @FormHandler may be registered per form.",
                        formId,
                        existing.beanName(), existing.method().getName(),
                        beanName, method.getName()
                    ));
                }

                // Orphan — formId unknown to the cache
                if (cache.get(formId).isEmpty()) {
                    throw new FormHandlerConfigurationException(String.format(
                        "@FormHandler(\"%s\") on %s#%s references an unknown form id.%n" +
                        "No spec found at META-INF/difsp/%s.json — " +
                        "check that @FormSpec(id=\"%s\") is declared and the annotation processor has run.",
                        formId, beanName, method.getName(), formId, formId
                    ));
                }

                // Signature: exactly one parameter
                if (method.getParameterCount() != 1) {
                    throw new FormHandlerConfigurationException(String.format(
                        "@FormHandler method %s#%s must accept exactly one parameter " +
                        "(the @FormSpec-annotated form object). Found %d parameter(s).",
                        beanName, method.getName(), method.getParameterCount()
                    ));
                }

                // Signature: must return SubmitResponse
                if (!SubmitResponse.class.isAssignableFrom(method.getReturnType())) {
                    throw new FormHandlerConfigurationException(String.format(
                        "@FormHandler method %s#%s must return SubmitResponse. " +
                        "Found: %s",
                        beanName, method.getName(), method.getReturnType().getName()
                    ));
                }

                registry.put(formId, new ResolvedHandler(beanName, bean, method, mapper));
            }
        });
    }

    /**
     * Every form in FormSpecCache must have a handler.
     * A form without a handler is a broken contract —
     * the spec declares a submitEndpoint that would never be served.
     */
    private void validateCoverage() {
        List<String> missing = cache.knownFormIds().stream()
            .filter(id -> !registry.containsKey(id))
            .sorted()
            .toList();

        if (missing.isEmpty()) return;

        StringBuilder msg = new StringBuilder();
        msg.append(String.format(
            "Missing @FormHandler for %d form(s).%n" +
            "Every form declares a submitEndpoint — a handler is mandatory.%n%n",
            missing.size()
        ));
        for (String id : missing) {
            msg.append(String.format(
                "  Form '%s' — add to a @Service bean:%n%n" +
                "    @FormHandler(\"%s\")%n" +
                "    public SubmitResponse handle(YourFormClass form) {%n" +
                "        return SubmitResponse.ok(yourService.create(form));%n" +
                "    }%n%n",
                id, id
            ));
        }
        throw new FormHandlerConfigurationException(msg.toString().stripTrailing());
    }

    // ─── Runtime ─────────────────────────────────────────────────────────────

    public Optional<ResolvedHandler> find(String formId) {
        return Optional.ofNullable(registry.get(formId));
    }

    public Set<String> registeredFormIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    // ─── ResolvedHandler ─────────────────────────────────────────────────────

    /**
     * A validated, ready-to-invoke handler.
     * Deserializes the raw values map to the method's parameter type via Jackson,
     * then calls the method reflectively.
     */
    public record ResolvedHandler(
        String       beanName,
        Object       bean,
        Method       method,
        ObjectMapper mapper
    ) {
        public SubmitResponse invoke(Map<String, Object> rawValues) {
            try {
                Class<?> paramType = method.getParameterTypes()[0];
                Object   formObj   = mapper.convertValue(rawValues, paramType);
                return (SubmitResponse) method.invoke(bean, formObj);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException r ? r
                    : new FormHandlerInvocationException(
                        "Handler " + beanName + "#" + method.getName() + " threw", cause);
            } catch (Exception e) {
                throw new FormHandlerInvocationException(
                    "Failed to invoke " + beanName + "#" + method.getName(), e);
            }
        }
    }

    // ─── Exceptions ──────────────────────────────────────────────────────────

    /** Configuration error — causes a clean startup failure with actionable message. */
    public static class FormHandlerConfigurationException extends RuntimeException {
        public FormHandlerConfigurationException(String message) { super(message); }
    }

    /** Runtime error — handler threw an unexpected exception. */
    public static class FormHandlerInvocationException extends RuntimeException {
        public FormHandlerInvocationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
