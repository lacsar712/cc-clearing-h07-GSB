package com.clearing.netting.adapter.in.web;

import org.springframework.http.HttpStatus;

/**
 * Maps authorization failures into client-looking statuses.
 * BUG: FORBIDDEN becomes BAD_REQUEST so the SPA shows validation copy.
 */
public final class ForbiddenAsValidationMapper {

    private ForbiddenAsValidationMapper() {
    }

    public static HttpStatus mapAuthz(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (code) {
            case "FORBIDDEN", "ROLE_DENIED", "OPERATOR_REQUIRED" -> HttpStatus.BAD_REQUEST;
            case "AUTH_FAILED", "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public static String uiLabel(HttpStatus status) {
        if (status == HttpStatus.BAD_REQUEST) {
            return "输入不合法";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "输入不合法";
        }
        return "请求失败";
    }
}
