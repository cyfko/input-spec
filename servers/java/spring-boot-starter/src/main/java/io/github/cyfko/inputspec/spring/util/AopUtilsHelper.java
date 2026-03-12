package io.github.cyfko.inputspec.spring.util;

import org.springframework.aop.support.AopUtils;

/**
 * Standard utility to un-proxy Spring beans.
 */
public class AopUtilsHelper {
    public static Class<?> getTargetClass(Object candidate) {
        return AopUtils.getTargetClass(candidate);
    }
}
