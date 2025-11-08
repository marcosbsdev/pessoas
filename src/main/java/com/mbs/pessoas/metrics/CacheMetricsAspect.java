package com.mbs.pessoas.metrics;

import java.lang.reflect.Method;
 

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Aspect
@Component
public class CacheMetricsAspect {

    private final CacheManager cacheManager;
    private final MeterRegistry registry;

    public CacheMetricsAspect(CacheManager cacheManager, MeterRegistry registry) {
        this.cacheManager = cacheManager;
        this.registry = registry;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable) && execution(* *(..))")
    public Object aroundCacheable(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        org.springframework.cache.annotation.Cacheable cacheable = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
        String[] cacheNames = cacheable.value();
    // key expression not evaluated here; we build a simple key from args

        // Build a simple key by using parameters when SpEL not available here
        Object[] args = pjp.getArgs();
        String key = (args != null && args.length > 0) ? String.valueOf(args[0]) : "all";

        // Only instrument the first cache name if present
        if (cacheNames != null && cacheNames.length > 0) {
            String cacheName = cacheNames[0];
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null && wrapper.get() != null) {
                    registry.counter("cache.requests", "cache", cacheName, "result", "hit").increment();
                    return wrapper.get();
                } else {
                    registry.counter("cache.requests", "cache", cacheName, "result", "miss").increment();
                }
            }
        }

        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = pjp.proceed();
            sample.stop(registry.timer("cache.method.latency", "method", method.getName()));
            return result;
        } catch (Throwable t) {
            sample.stop(registry.timer("cache.method.latency", "method", method.getName()));
            throw t;
        }
    }
}
