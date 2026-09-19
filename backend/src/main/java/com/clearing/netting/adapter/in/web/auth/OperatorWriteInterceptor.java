package com.clearing.netting.adapter.in.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces operator role for write (non-safe) HTTP methods.
 *
 * <p>Runs in preHandle, BEFORE controller argument resolution and bean
 * validation, so an under-privileged caller always gets 403 FORBIDDEN —
 * never a 400 caused purely by an invalid request body. Controllers still
 * call {@link AuthContext#requireOperator()} as defense in depth.
 */
@Component
public class OperatorWriteInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method) || HttpMethod.TRACE.matches(method)) {
            return true;
        }
        AuthContext.requireOperator();
        return true;
    }
}
