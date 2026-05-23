package com.facketmaster.controller;

import com.facketmaster.controller.request.UserRequest;
import com.facketmaster.controller.response.UserResponse;
import com.facketmaster.entity.User;
import com.facketmaster.mapper.UserMapper;
import com.facketmaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request){
        User newUser = UserMapper.toUser(request);
        User savedUser = userService.saveUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.toUserResponse(savedUser));
    }
}
