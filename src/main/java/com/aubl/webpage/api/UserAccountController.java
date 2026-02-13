package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.IdResponse;
import com.aubl.webpage.api.dto.UserSignupRequest;
import com.aubl.webpage.domain.entity.UserAccount;
import com.aubl.webpage.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/signup")
    public ResponseEntity<IdResponse> signup(@RequestBody UserSignupRequest request) {
        UserAccount account = userAccountService.signup(request);
        return ResponseEntity.ok(new IdResponse(account.getId()));
    }
}
