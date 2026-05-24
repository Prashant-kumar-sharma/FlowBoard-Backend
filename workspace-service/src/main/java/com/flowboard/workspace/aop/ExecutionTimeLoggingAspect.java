package com.flowboard.workspace.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class ExecutionTimeLoggingAspect {

    @Around("within(com.flowboard.workspace.controller..*) || within(com.flowboard.workspace.service..*)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("Executed {} in {} ms", joinPoint.getSignature().toShortString(), stopWatch.getTotalTimeMillis());
        }
    }
}
