package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.FormSpec;
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
 *   <li>A form in FormSpecCache has no {@code @FormHandler}  (broken contract)</li>
 *   <li>Two {@code @FormHandler}s target the same formId     (ambiguous routing)</li>
 *   <li>A {@code @FormHandler} targets an unknown formId     (orphan handler)</li>
 *   <li>A {@code @FormHandler} method has wrong signature    (not exactly 1 param + SubmitResponse)</li>
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
            Class<?> targetClass = AopUtils.getTargetClass(bean);

            for (Method method : targetClass.getMethods()) {
                if (method.getAnnotation(FormHandler.class) == null) continue;

                // ── Signature: exactly one parameter ─────────────────────────
                if (method.getParameterCount() != 1) {
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Invalid @FormHandler signature%n" +
                                    "***************************%n%n" +
                                    "  Method  : %s#%s%n" +
                                    "  Problem : declares %d parameter(s) — exactly 1 is required.%n%n" +
                                    "  Fix     : The method must accept exactly one parameter,%n" +
                                    "            the @FormSpec-annotated form class.%n%n" +
                                    "    @FormHandler%n" +
                                    "    public SubmitResponse %s(YourFormClass form) { ... }%n",
                            beanName, method.getName(), method.getParameterCount(),
                            method.getName()
                    ));
                }

                // ── Signature: must return SubmitResponse ─────────────────────
                if (!SubmitResponse.class.isAssignableFrom(method.getReturnType())) {
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Invalid @FormHandler return type%n" +
                                    "***************************%n%n" +
                                    "  Method  : %s#%s%n" +
                                    "  Problem : returns '%s' — SubmitResponse is required.%n%n" +
                                    "  Fix     : Change the return type and use%n" +
                                    "            SubmitResponse.ok(...) or SubmitResponse.rejected(...).%n%n" +
                                    "    @FormHandler%n" +
                                    "    public SubmitResponse %s(YourFormClass form) { ... }%n",
                            beanName, method.getName(), method.getReturnType().getSimpleName(),
                            method.getName()
                    ));
                }

                // ── Infer formId from @FormSpec on the parameter type ─────────
                Class<?> paramType = method.getParameterTypes()[0];
                String   formId    = getFormSpecId(beanName, method, paramType);

                // ── Duplicate handler for the same form ───────────────────────
                if (registry.containsKey(formId)) {
                    ResolvedHandler existing = registry.get(formId);
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Duplicate @FormHandler%n" +
                                    "***************************%n%n" +
                                    "  Form    : '%s'%n" +
                                    "  Problem : two handlers are registered for the same form.%n%n" +
                                    "  Handler 1 : %s#%s%n" +
                                    "  Handler 2 : %s#%s%n%n" +
                                    "  Fix     : Remove one of the two @FormHandler methods.%n" +
                                    "            Exactly one handler per form is allowed.%n",
                            formId,
                            existing.beanName(), existing.method().getName(),
                            beanName, method.getName()
                    ));
                }

                // ── Orphan — formId not known to the cache ────────────────────
                if (cache.get(formId).isEmpty()) {
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Unknown form id in @FormHandler%n" +
                                    "***************************%n%n" +
                                    "  Method  : %s#%s%n" +
                                    "  Form id : '%s' (inferred from @FormSpec on %s)%n" +
                                    "  Problem : no spec was found for this form id.%n%n" +
                                    "  Fix     : Check that the annotation processor has run and that%n" +
                                    "            META-INF/input-spec/%s.json was generated.%n" +
                                    "            Also verify that @FormSpec(id = \"%s\") on %s%n" +
                                    "            matches an existing spec file exactly.%n",
                            beanName, method.getName(),
                            formId, paramType.getSimpleName(),
                            formId, formId, paramType.getSimpleName()
                    ));
                }

                registry.put(formId, new ResolvedHandler(beanName, bean, method, mapper));
            }
        });
    }

    private static String getFormSpecId(String beanName, Method method, Class<?> paramType) {
        FormSpec formSpec = paramType.getAnnotation(FormSpec.class);
        if (formSpec == null) {
            throw new FormHandlerConfigurationException(String.format(
                    "%n" +
                            "***************************%n" +
                            "DIFSP — Missing @FormSpec on @FormHandler parameter%n" +
                            "***************************%n%n" +
                            "  Method  : %s#%s%n" +
                            "  Problem : parameter type '%s' is not annotated with @FormSpec.%n" +
                            "            The form id is inferred from the parameter class — not declared%n" +
                            "            explicitly on @FormHandler.%n%n" +
                            "  Fix     : Annotate the parameter class with @FormSpec:%n%n" +
                            "    @FormSpec(id = \"your-form-id\")%n" +
                            "    public class %s { ... }%n",
                    beanName, method.getName(),
                    paramType.getSimpleName(),
                    paramType.getSimpleName()
            ));
        }
        return formSpec.id();
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
                "%n" +
                        "***************************%n" +
                        "DIFSP — Missing @FormHandler%n" +
                        "***************************%n%n" +
                        "  Problem : %d form(s) have no registered @FormHandler.%n" +
                        "            Every form must have exactly one handler.%n%n",
                missing.size()
        ));
        for (String id : missing) {
            msg.append(String.format(
                    "  ▸ Form '%s'%n" +
                            "    Add to a @Service bean:%n%n" +
                            "      @FormSpec(id = \"%s\")%n" +
                            "      public class YourFormClass { ... }%n%n" +
                            "      @FormHandler%n" +
                            "      public SubmitResponse handle(YourFormClass form) {%n" +
                            "          return SubmitResponse.ok(yourService.create(form));%n" +
                            "      }%n%n",
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