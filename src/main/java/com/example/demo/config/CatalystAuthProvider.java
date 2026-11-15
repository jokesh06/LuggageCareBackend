package com.example.demo.config;

import com.zc.auth.AuthHeaderProvider;
import jakarta.servlet.http.HttpServletRequest;

public class CatalystAuthProvider implements AuthHeaderProvider {

    private final HttpServletRequest request;

    public CatalystAuthProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getHeaderValue(String key) {

        System.out.println("hiiii");

        String value = request.getHeader(key);

   System.out.println(
        "CATALYST_HEADER | " +
        "key=" + key +
        " | present=" + (value != null) +
        " | blank=" + (value == null || value.isBlank()) +
        " | length=" + (value == null ? 0 : value.length())
    );

        return value;
    }
}