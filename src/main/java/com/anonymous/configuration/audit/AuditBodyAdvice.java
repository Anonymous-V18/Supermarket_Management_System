package com.anonymous.configuration.audit;

import com.anonymous.dto.request.LoginRequest;
import com.anonymous.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Type;

// Hands the audit interceptor the few facts it can't read from the request itself
public final class AuditBodyAdvice {

    private static final int SUCCESS_CODE = 10000;

    private AuditBodyAdvice() {
    }

    // The username typed on the sign-in form, and the ids sent to a delete endpoint
    @ControllerAdvice
    public static class RequestSide extends RequestBodyAdviceAdapter {

        @Override
        public boolean supports(MethodParameter methodParameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                    Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                if (body instanceof LoginRequest login) {
                    request.setAttribute(AuditInterceptor.ATTR_USERNAME, login.getUsername());
                } else if (body instanceof String[] ids) {
                    request.setAttribute(AuditInterceptor.ATTR_REQUEST_REFERENCE, String.join(",", ids));
                }
            }
            return body;
        }
    }

    // The code (or id) of the document a request created or changed, and the error code of a failure
    @ControllerAdvice
    public static class ResponseSide implements ResponseBodyAdvice<Object> {

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                      ServerHttpRequest request, ServerHttpResponse response) {
            if (body instanceof ApiResponse<?> apiResponse && request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                if (apiResponse.getCode() != null && apiResponse.getCode() != SUCCESS_CODE) {
                    httpRequest.setAttribute(AuditInterceptor.ATTR_ERROR_CODE, apiResponse.getCode());
                }
                String reference = referenceOf(apiResponse.getResult());
                if (reference != null) {
                    httpRequest.setAttribute(AuditInterceptor.ATTR_RESULT_REFERENCE, reference);
                }
            }
            return body;
        }

        private static String referenceOf(Object result) {
            if (result == null || result instanceof String || result instanceof Iterable<?>) {
                return null;
            }
            BeanWrapperImpl bean = new BeanWrapperImpl(result);
            for (String property : new String[]{"code", "id"}) {
                if (bean.isReadableProperty(property) && bean.getPropertyValue(property) != null) {
                    return bean.getPropertyValue(property).toString();
                }
            }
            return null;
        }
    }

}
