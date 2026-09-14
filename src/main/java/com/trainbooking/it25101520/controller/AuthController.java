package com.trainbooking.it25101520.controller;
import com.trainbooking.it25101520.service.*;
import com.trainbooking.it25101520.model.*;
import com.trainbooking.it25101520.dto.*;
import com.trainbooking.it25101520.repository.*;

import com.trainbooking.it25101520.dto.LoginRequest;
import com.trainbooking.it25101520.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling authentication views and form submissions (login, register, forgot-password).
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Displays the login page.
     *
     * @param model UI model
     * @return 'auth/login' template name
     */
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        log.debug("Displaying login page");
        // TODO: Add any necessary attributes (e.g. login error flags) to model
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "it25101520/login";
    }

    /**
     * Displays the registration page.
     *
     * @param model UI model
     * @return 'auth/register' template name
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.debug("Displaying registration page");
        // TODO: Prepare registration model object
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "it25101520/register";
    }

    /**
     * Processes user registration form submission.
     *
     * @param registerRequest form payload
     * @param bindingResult validation result
     * @param model UI model
     * @return redirect to login on success or registration view on validation error
     */
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model
    ) {
        log.info("Handling registration submission for email: {}", registerRequest.getEmail());
        if (bindingResult.hasErrors()) {
            return "it25101520/register";
        }
        // TODO: Call authService.registerUser(registerRequest) and handle duplicate email errors
        authService.registerUser(registerRequest);
        return "redirect:/login?registered=true";
    }

    /**
     * Displays the forgot password page.
     *
     * @param model UI model
     * @return 'auth/forgot-password' template name
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        log.debug("Displaying forgot password page");
        // TODO: Prepare forgot password form model if needed
        return "it25101520/forgot-password";
    }

    /**
     * Handles forgot password reset email request.
     *
     * @param email user email address
     * @param model UI model
     * @return 'auth/forgot-password' template with status message
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        log.info("Handling password reset request for email: {}", email);
        // TODO: Call authService.sendPasswordResetEmail(email) and attach confirmation message
        authService.sendPasswordResetEmail(email);
        model.addAttribute("successMessage", "Password reset instructions have been sent to your email.");
        return "it25101520/forgot-password";
    }
}
