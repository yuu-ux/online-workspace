package com.example.online_workspace.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.online_workspace.form.SignUpForm;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * UserController
 */
@Controller
@RequestMapping("/users")
public class UserController {
    @GetMapping("/")
        public String index() {
            return "index";
        }

    @GetMapping("/sign_up")
        public String signUp(Model model) {
            SignUpForm signUpForm = new SignUpForm();
            model.addAttribute("signUpForm", signUpForm);
            return "users/sign-up";
        }

    @PostMapping("/sign_up")
        public String signUp() {
            return "redirect:/";
        }
}

