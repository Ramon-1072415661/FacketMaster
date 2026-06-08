package com.facketmaster.event.controller;

import com.facketmaster.config.TokenService;
import com.facketmaster.controller.request.LoginRequest;
import com.facketmaster.controller.request.UserRequest;
import com.facketmaster.controller.response.UserResponse;
import com.facketmaster.entity.User;
import com.facketmaster.mapper.UserMapper;
import com.facketmaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        User newUser = UserMapper.toUser(request);
        User savedUser = userService.saveUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.toUserResponse(savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication auth = authManager.authenticate(userAndPass);

        User user = (User) auth.getPrincipal();

        return ResponseEntity.ok(tokenService.generateToken(user));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<UserResponse> createAdmin(@RequestBody UserRequest request) {
        User newUser = UserMapper.toUser(request);
        User savedUser = userService.saveAdmin(newUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.toUserResponse(savedUser));
    }
}