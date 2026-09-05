package com.example.online_workspace.controllers.auth;

import org.springframework.http.ResponseEntity;
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
     * CSRFトークンをCookieへ保存し、レスポンス本文なしで返す。
     *
     * @param csrfToken Spring Securityが遅延生成するCSRFトークン
     * @return 本文なしの204 No Contentレスポンス
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }
}
