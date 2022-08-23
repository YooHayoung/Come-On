package com.comeon.userservice.common.argresolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Map;

@RequiredArgsConstructor
public class JwtArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasUserIdAnnotation = parameter.hasParameterAnnotation(CurrentUserId.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());
        return hasUserIdAnnotation && isLongType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String accessToken = resolveAccessToken(request);
        return getUserId(accessToken);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!(StringUtils.hasText(token) && token.startsWith("Bearer "))) {
            // TODO 예외 처리
            throw new RuntimeException("토큰 없음");
        }
        return token.substring(7);
    }

    private Long getUserId(String accessToken) {
        Base64.Decoder urlDecoder = Base64.getUrlDecoder();
        String payload = new String(urlDecoder.decode(accessToken.split("\\.")[1]));
        Map<String, Object> payloadObject = null;
        try {
            payloadObject = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Long.parseLong((String) payloadObject.get("sub"));
    }
}