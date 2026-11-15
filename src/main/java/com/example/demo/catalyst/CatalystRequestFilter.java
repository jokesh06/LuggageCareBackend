package com.example.demo.catalyst;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zc.auth.AuthHeaderProvider;
import com.zc.auth.CatalystSDK;
import org.springframework.core.Ordered;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // run before anything else
public class CatalystRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            // Only requests that came through Catalyst carry the project id header.
            if (request.getHeader(CatalystSDK.PROJECTID_HEADER_KEY) != null) {

                // 1. Give the SDK a way to read this request's headers. The SDK calls
                //    getHeaderValue("x-zc-…") whenever it needs one of Catalyst's values.
                CatalystSDK.init(new RequestHeaders(request));

                // 2. Open our project for this request (with server rights, and the user's sign-in).
                CatalystContext.open();
            }

            // 3. Carry on to the controller (and the rest of the request).
            chain.doFilter(request, response);

        } finally {
            // 4. Always, even when something failed: forget this request's keys.
            CatalystContext.close();
        }
    }

    /**
     * The small "adapter" the SDK asks for: given a header name, return its value from this
     * request. (Written as a normal class instead of a shortcut, so it is easy to read.)
     */
    private static class RequestHeaders implements AuthHeaderProvider {

        private final HttpServletRequest request;

        RequestHeaders(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getHeaderValue(String name) {
            return request.getHeader(name);
        }
    }
}
