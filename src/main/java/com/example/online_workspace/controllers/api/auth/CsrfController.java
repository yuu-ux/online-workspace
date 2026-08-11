package com.example.online_workspace.controllers.api.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRFトークンを提供するREST API。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    /**
     * 現在のセッションで利用するCSRFトークンを返す。
     *
     * @param csrfToken Spring Securityが生成したCSRFトークン
     * @return CSRFトークン
     */
    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }
}
