package io.github.cyfko.inputspec.spring.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.spring.FormHandler;
import io.github.cyfko.inputspec.spring.SubmitResponse;
import io.github.cyfko.inputspec.spring.spi.FormHandlerProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Registry of all form handlers in the application.
 *
 * Built exactly once when the Spring context is fully initialized
 * ({@link SmartInitializingSingleton}), before the application accepts traffic.
 *
 * <h3>Resolution strategy (primary / fallback)</h3>
 * <ol>
 *   <li><strong>Primary</strong> — a method annotated with {@code @FormHandler} whose
 *       parameter type carries {@code @FormSpec(id = formId)}.</li>
 *   <li><strong>Fallback</strong> — a {@link FormHandlerProvider} bean that declares
 *       the formId in {@link FormHandlerProvider#getSupportedForms()}.</li>
 * </ol>
 * If both exist for the same formId, the {@code @FormHandler} wins silently.
 *
 * <h3>Startup guarantees — application refuses to start if:</h3>
 * <ol>
 *   <li>A form in FormSpecCache has neither a {@code @FormHandler} nor a provider.</li>
 *   <li>Two {@code @FormHandler}s target the same formId (ambiguous routing).</li>
 *   <li>Two {@link FormHandlerProvider}s declare the same formId (ambiguous fallback).</li>
 *   <li>A {@code @FormHandler} targets an unknown formId (orphan handler).</li>
 *   <li>A {@code @FormHandler} method has the wrong signature.</li>
 * </ol>
 *
 * At runtime, {@link #find(String)} is a plain Map lookup — O(1).
 */
public class FormHandlerRegistry implements SmartInitializingSingleton {

    private final ApplicationContext context;
    private final FormSpecCache      cache;
    private final ObjectMapper       mapper;

    /** Primary handlers — keyed by formId. */
    private final Map<String, ResolvedHandler>      registry         = new HashMap<>();

    /** Fallback providers — keyed by formId for O(1) lookup. */
    private final Map<String, FormHandlerProvider>  fallbackRegistry = new HashMap<>();

    /** Tracks which provider bean covers each formId — used for conflict error messages. */
    private final Map<String, String>               providerBeanNames = new HashMap<>();

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
        scanHandlerProviders();
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

                // ── Duplicate @FormHandler for the same form ──────────────────
                if (registry.containsKey(formId)) {
                    ResolvedHandler existing = registry.get(formId);
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Duplicate @FormHandler%n" +
                                    "***************************%n%n" +
                                    "  Form      : '%s'%n" +
                                    "  Problem   : two @FormHandler methods are registered for the same form.%n%n" +
                                    "  Handler 1 : %s#%s%n" +
                                    "  Handler 2 : %s#%s%n%n" +
                                    "  Fix       : Remove one of the two @FormHandler methods.%n" +
                                    "              Exactly one handler per form is allowed.%n",
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

    private void scanHandlerProviders() {
        context.getBeansOfType(FormHandlerProvider.class).forEach((beanName, provider) -> {
            for (String formId : provider.getSupportedForms()) {

                // ── Conflict between two providers ────────────────────────────
                if (fallbackRegistry.containsKey(formId)) {
                    throw new FormHandlerConfigurationException(String.format(
                            "%n" +
                                    "***************************%n" +
                                    "DIFSP — Duplicate FormHandlerProvider%n" +
                                    "***************************%n%n" +
                                    "  Form id    : '%s'%n" +
                                    "  Problem    : two FormHandlerProvider beans both declare support%n" +
                                    "               for this form id — ambiguous fallback routing.%n%n" +
                                    "  Provider 1 : %s%n" +
                                    "  Provider 2 : %s%n%n" +
                                    "  Fix        : Each form id must be declared by exactly one%n" +
                                    "               FormHandlerProvider. Remove the duplicate declaration%n" +
                                    "               from one of the two providers.%n",
                            formId,
                            providerBeanNames.get(formId),
                            beanName
                    ));
                }

                // ── @FormHandler already covers this id — provider is shadowed ─
                // Silent: @FormHandler is primary, provider is fallback by design.

                fallbackRegistry.put(formId, provider);
                providerBeanNames.put(formId, beanName);
            }
        });
    }

    /**
     * Every form in FormSpecCache must be covered by either a {@code @FormHandler}
     * or a {@link FormHandlerProvider}. A form with no handler at all is a broken
     * contract — the spec declares a submitEndpoint that would never be served.
     */
    private void validateCoverage() {
        List<String> missing = cache.knownFormIds().stream()
                .filter(id -> !registry.containsKey(id) && !fallbackRegistry.containsKey(id))
                .sorted()
                .toList();

        if (missing.isEmpty()) return;

        StringBuilder msg = new StringBuilder();
        msg.append(String.format(
                "%n" +
                        "***************************%n" +
                        "DIFSP — Missing handler%n" +
                        "***************************%n%n" +
                        "  Problem : %d form(s) have no registered handler.%n" +
                        "            Each form must be covered by a @FormHandler method%n" +
                        "            or a FormHandlerProvider bean.%n%n",
                missing.size()
        ));
        for (String id : missing) {
            msg.append(String.format(
                    "  ▸ Form '%s'%n" +
                            "    Option 1 — dedicated @FormHandler:%n%n" +
                            "      @FormSpec(id = \"%s\")%n" +
                            "      public class YourFormClass { ... }%n%n" +
                            "      @FormHandler%n" +
                            "      public SubmitResponse handle(YourFormClass form) {%n" +
                            "          return SubmitResponse.ok(yourService.process(form));%n" +
                            "      }%n%n" +
                            "    Option 2 — FormHandlerProvider (for contributed/dynamic forms):%n%n" +
                            "      @Component%n" +
                            "      public class YourProvider implements FormHandlerProvider {%n" +
                            "          public Set<String> getSupportedForms() { return Set.of(\"%s\"); }%n" +
                            "          public SubmitResponse submit(String id, Map<String,Object> form) { ... }%n" +
                            "      }%n%n",
                    id, id, id
            ));
        }
        throw new FormHandlerConfigurationException(msg.toString().stripTrailing());
    }

    // ─── Runtime ─────────────────────────────────────────────────────────────

    /**
     * Resolves the handler for the given formId.
     *
     * Resolution order:
     * <ol>
     *   <li>Primary — {@code @FormHandler} method registered at startup.</li>
     *   <li>Fallback — {@link FormHandlerProvider} that declared this formId.</li>
     * </ol>
     *
     * @return an {@link Optional} containing either a {@link ResolvedHandler}
     *         or a {@link ResolvedProvider}, or empty if no handler exists.
     */
    public Optional<HandlerResolution> find(String formId) {
        ResolvedHandler handler = registry.get(formId);
        if (handler != null) return Optional.of(handler);

        FormHandlerProvider provider = fallbackRegistry.get(formId);
        if (provider != null) return Optional.of(new ResolvedProvider(provider));

        return Optional.empty();
    }

    public Set<String> registeredFormIds() {
        Set<String> all = new HashSet<>(registry.keySet());
        all.addAll(fallbackRegistry.keySet());
        return Collections.unmodifiableSet(all);
    }

    // ─── Handler resolution sealed hierarchy ─────────────────────────────────

    /**
     * Sealed interface representing a resolved handler — either a primary
     * {@link ResolvedHandler} or a fallback {@link ResolvedProvider}.
     *
     * <p>The controller calls these two methods in order for every submission:</p>
     * <ol>
     *   <li>{@link #validate} — business/stateful validation (after stateless
     *       validation by {@code FormSpecValidator} has already passed).
     *       Stop and return the rejection if this fails.</li>
     *   <li>{@link #invoke} — execute the business action.</li>
     * </ol>
     */
    public sealed interface HandlerResolution
            permits ResolvedHandler, ResolvedProvider {

        /**
         * Performs business/stateful validation.
         *
         * <p>For a primary {@link ResolvedHandler} ({@code @FormHandler}),
         * this is a no-op — the handler itself is responsible for all logic.
         * For a fallback {@link ResolvedProvider}, this delegates to
         * {@link FormHandlerProvider#validate}.</p>
         *
         * @return {@link SubmitResponse#ok()} if validation passes,
         *         or a rejected response otherwise
         */
        default SubmitResponse validate(String formId, Map<String, Object> rawValues) {
            return SubmitResponse.ok(); // no-op for @FormHandler — override in ResolvedProvider
        }

        /**
         * Executes the business action for the submission.
         * Only called after {@link #validate} has returned {@link SubmitResponse#ok()}.
         */
        SubmitResponse invoke(String formId, Map<String, Object> rawValues);
    }

    // ─── ResolvedHandler ─────────────────────────────────────────────────────

    /**
     * Primary resolution — a validated {@code @FormHandler} method.
     * Deserializes the raw values map into the declared parameter type via Jackson,
     * then invokes the method reflectively.
     *
     * <p>{@link #validate} is intentionally not overridden — {@code @FormHandler}
     * methods are the single responsibility point for all business logic and do not
     * have a separate validation phase.</p>
     */
    public record ResolvedHandler(
            String       beanName,
            Object       bean,
            Method       method,
            ObjectMapper mapper
    ) implements HandlerResolution {

        @Override
        public SubmitResponse invoke(String formId, Map<String, Object> rawValues) {
            try {
                method.setAccessible(true); // required for inner classes and CGLIB proxies
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

    // ─── ResolvedProvider ────────────────────────────────────────────────────

    /**
     * Fallback resolution — a {@link FormHandlerProvider} that declared support
     * for the formId.
     *
     * <p>{@link #validate} delegates to {@link FormHandlerProvider#validate}.
     * {@link #invoke} delegates to {@link FormHandlerProvider#submit}.
     * The controller guarantees that {@link #invoke} is never called if
     * {@link #validate} returned a rejected response.</p>
     */
    public record ResolvedProvider(
            FormHandlerProvider provider
    ) implements HandlerResolution {

        @Override
        public SubmitResponse validate(String formId, Map<String, Object> rawValues) {
            return provider.validate(formId, rawValues);
        }

        @Override
        public SubmitResponse invoke(String formId, Map<String, Object> rawValues) {
            return provider.submit(formId, rawValues);
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