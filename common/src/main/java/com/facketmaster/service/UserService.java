package com.facketmaster.service;

import com.facketmaster.entity.Role;
import com.facketmaster.entity.User;
import com.facketmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User saveUser(User user) {
        String password = user.getPassword();
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode((password)));
        return userRepository.save(user);
    }

    public User saveAdmin(User user) {
        String password = user.getPassword();
        user.setRole(Role.ADMIN);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

}