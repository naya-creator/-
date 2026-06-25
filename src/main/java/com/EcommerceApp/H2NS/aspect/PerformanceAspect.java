package com.EcommerceApp.H2NS.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

// import java.util.Arrays;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger benchmarkLogger = LoggerFactory.getLogger("BENCHMARK_LOGGER");

    @Pointcut("execution(* com.EcommerceApp.H2NS.service.OrderService.*(..))")
    public void orderServiceMethods() {
    }

    @Pointcut("execution(* com.EcommerceApp.H2NS.service.UserService.*(..))")
    public void userServiceMethods() {
    }

    @Pointcut("execution(* com.EcommerceApp.H2NS.service.InventoryService.*(..))")
    public void inventoryServiceMethods() {
    }

    @Pointcut("execution(* com.EcommerceApp.H2NS.repository.*.*(..))")
    public void repositoryMethods() {
    }

    @Pointcut("orderServiceMethods() || userServiceMethods() || inventoryServiceMethods() || repositoryMethods()")
    public void allServiceAndRepoMethods() {
    }

    @Around("allServiceAndRepoMethods()")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        Object[] args = joinPoint.getArgs();
        String argsInfo = extractArgsInfo(args);

        String userId = extractUserId(args);

        String previousMDC = MDC.get("methodName");
        double cpuBefore = getCpuLoad();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start(methodName);

        Object result;
        try {
            MDC.put("methodName", methodName);

            result = joinPoint.proceed();

            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();

            double cpuAfter = getCpuLoad();
            double cpuConsumed = cpuAfter - cpuBefore;
            if (cpuConsumed < 0)
                cpuConsumed = 0;

            benchmarkLogger.info("{} | UserID: {} | Args: {} | Duration: {} ms | CPU Load: {} %",
                    methodName, userId, argsInfo, elapsed, String.format("%.2f", cpuConsumed));

            if (elapsed > 200) {
                benchmarkLogger.warn("[BOTTLENECK DETECTED] {} | UserID: {} | Args: {} | Duration: {} ms | CPU Load: {} %",
                        methodName, userId, argsInfo, elapsed, String.format("%.2f", cpuConsumed));
            }

            return result;

        } catch (Throwable throwable) {
            stopWatch.stop();
            long elapsed = stopWatch.getTotalTimeMillis();

            double cpuAfter = getCpuLoad();
            double cpuConsumed = cpuAfter - cpuBefore;
            if (cpuConsumed < 0)
                cpuConsumed = 0;

            benchmarkLogger.warn("[FAILED] {} | UserID: {} | Args: {} | Duration: {} ms | CPU Load: {} % | Exception: {}",
                    methodName, userId, argsInfo, elapsed, String.format("%.2f", cpuConsumed) , throwable.getMessage());
            throw throwable;

        } finally {
            if (previousMDC != null) {
                MDC.put("methodName", previousMDC);
            } else {
                MDC.remove("methodName");
            }
        }
    }

    private String extractUserId(Object[] args) {
        if (args == null)
            return "N/A";
        for (Object arg : args) {
            if (arg instanceof Long) {
                return String.valueOf(arg);
            }
        }
        return "N/A";
    }

    private String extractArgsInfo(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (i > 0)
                sb.append(", ");
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof Long || arg instanceof Integer || arg instanceof String) {
                sb.append(arg);
            } else {
                sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private double getCpuLoad() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad() * 100;
        } catch (Exception e) {
            return -1.0;
        }
    }
}