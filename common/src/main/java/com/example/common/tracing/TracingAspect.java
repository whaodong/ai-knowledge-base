package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * 追踪切面
 * 
 * <p>自动为带有@WithSpan注解的方法添加追踪。</p>
 */
@Slf4j
@Aspect
@Component
public class TracingAspect {

    @Autowired
    private Tracer tracer;

    @Autowired
    private TraceAnalysisService traceAnalysisService;

    /**
     * 拦截所有Service层方法，添加基础追踪
     */
    @Around("execution(* com.example..service..*.*(..))")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String spanName = className + "." + methodName;
        
        Span span = tracer.spanBuilder()
                .name(spanName)
                .kind(Span.Kind.SERVER)
                .start();
        
        Instant startTime = Instant.now();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 添加方法参数标签
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            if (paramNames != null && args != null) {
                for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                    Object arg = args[i];
                    if (arg != null && !isComplexType(arg.getClass())) {
                        span.tag("arg." + paramNames[i], String.valueOf(arg));
                    }
                }
            }
            
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 添加返回值标签（简单类型）
            if (result != null && !isComplexType(result.getClass())) {
                span.tag("result", String.valueOf(result));
            }
            
            return result;
            
        } catch (Throwable ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
            
            // 记录统计信息
            Duration duration = Duration.between(startTime, Instant.now());
            traceAnalysisService.recordSpanCompletion(span, duration);
        }
    }

    /**
     * 拦截Repository层方法，添加数据库访问追踪
     */
    @Around("execution(* com.example..repository..*.*(..))")
    public Object traceRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String spanName = "db." + className + "." + methodName;
        
        Span span = tracer.spanBuilder()
                .name(spanName)
                .kind(Span.Kind.CLIENT)
                .start();
        
        span.remoteServiceName("database");
        
        Instant startTime = Instant.now();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
            
            Duration duration = Duration.between(startTime, Instant.now());
            traceAnalysisService.recordSpanCompletion(span, duration);
        }
    }

    /**
     * 判断是否为复杂类型（不记录详细值）
     */
    private boolean isComplexType(Class<?> type) {
        return type.isArray() || 
               type.isAssignableFrom(java.util.Collection.class) ||
               type.isAssignableFrom(java.util.Map.class) ||
               type.getName().contains("com.example.");
    }
}
