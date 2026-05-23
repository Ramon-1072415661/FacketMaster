package com.facketmaster.service;

import com.facketmaster.entity.Role;
import com.facketmaster.entity.User;
import com.facketmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public User saveUser(User user){
       return userRepository.save(user);
    }
}
