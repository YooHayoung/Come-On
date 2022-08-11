package com.comeon.authservice.auth.filter;

import com.comeon.authservice.auth.jwt.exception.InvalidAccessTokenException;
import com.comeon.authservice.auth.jwt.exception.JwtNotExistException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JwtAuthenticationExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtNotExistException e) {
            errorResponse(response, e.getMessage());
        } catch (InvalidAccessTokenException e) {
            errorResponse(response, "Invalid Access Token");
        } catch (JwtException e) {
            errorResponse(response, "Invalid Refresh Token");
        }
    }

    private void errorResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // TODO Use ObjectMapper
        response.getWriter().write("{\"error\":\"" + "error" + "\",\"message\":\"" + message + "\"}");
    }
}