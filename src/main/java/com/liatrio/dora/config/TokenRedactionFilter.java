package com.liatrio.dora.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Servlet filter that redacts the {@code token} query parameter value from
 * any log output. The wrapper is applied to every request so that framework
 * logging (e.g., access logs) never records a real GitHub PAT.
 */
@Component
public class TokenRedactionFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TokenRedactionFilter.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("((?:^|&)token=)[^&]*");
    private static final String TOKEN_REPLACEMENT = "$1REDACTED";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletRequestWrapper redacted = new HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getQueryString() {
                String qs = super.getQueryString();
                if (qs == null) return null;
                return TOKEN_PATTERN.matcher(qs).replaceAll(TOKEN_REPLACEMENT);
            }
        };

        log.debug("Request: {} {}", redacted.getMethod(), redacted.getRequestURI());
        chain.doFilter(redacted, response);
    }
}
