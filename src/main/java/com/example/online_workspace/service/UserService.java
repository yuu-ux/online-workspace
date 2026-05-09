package com.example.online_workspace.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.entity.UserEntity;
import com.example.online_workspace.form.SignUpForm;
import com.example.online_workspace.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(SignUpForm form) {
        if (!form.getPassword().equals(form.getPasswordConfirmation())) {
            throw new IllegalArgumentException("確認用パスワードが一致しません。");
        }
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            throw new IllegalArgumentException("このメールアドレスは既に使用されています。");
        }

        UserEntity user = new UserEntity();
        user.setName(form.getName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.insert(user);
        return user;
    }
}
