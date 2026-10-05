package com.trainbooking.it25101520.controller;

import com.trainbooking.it25101520.dto.LoginRequest;
import com.trainbooking.it25101520.dto.RegisterRequest;
import com.trainbooking.it25101520.service.AuthService;
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
     * @param registered optional parameter indicating successful registration
     * @param model UI model
     * @return 'it25101520/login' template name
     */
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
            Model model
    ) {
        log.debug("Displaying login page");
        if ("true".equals(registered)) {
            model.addAttribute("successMessage", "Registration successful! You may now sign in.");
        }
        if ("true".equals(resetSuccess)) {
            model.addAttribute("successMessage", "Password reset successfully! You can now sign in with your new password.");
        }
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "it25101520/login";
    }

    /**
     * Displays the registration page.
     *
     * @param model UI model
     * @return 'it25101520/register' template name
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.debug("Displaying registration page");
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
        try {
            authService.registerUser(registerRequest);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            log.warn("Registration rejected: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            bindingResult.rejectValue("email", "error.registerRequest", e.getMessage());
            return "it25101520/register";
        }
    }

    /**
     * Displays the forgot password page.
     *
     * @param model UI model
     * @return 'it25101520/forgot-password' template name
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        log.debug("Displaying forgot password page");
        return "it25101520/forgot-password";
    }

    /**
     * Handles forgot password reset email request.
     *
     * @param email user email address
     * @param model UI model
     * @return 'it25101520/forgot-password' template with status message
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        log.info("Handling password reset request for email: {}", email);
        authService.sendPasswordResetEmail(email);
        model.addAttribute("successMessage", "Password reset instructions have been generated. If registered, check your email.");
        return "it25101520/forgot-password";
    }

    /**
     * Displays password reset page with token.
     *
     * @param token reset token
     * @param model UI model
     * @return 'it25101520/reset-password' template name
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token, Model model) {
        log.debug("Displaying reset password page with token: {}", token);
        if (token == null || !authService.validatePasswordResetToken(token)) {
            model.addAttribute("errorMessage", "Password reset link is invalid or has expired. Please request a new one.");
            return "it25101520/forgot-password";
        }
        model.addAttribute("token", token);
        return "it25101520/reset-password";
    }

    /**
     * Processes password reset form submission.
     *
     * @param token reset token
     * @param password new password
     * @param confirmPassword password confirmation
     * @param model UI model
     * @return redirect to login on success or reset-password view on mismatch/failure
     */
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model
    ) {
        log.info("Processing password reset with token: {}", token);
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match. Please re-enter.");
            return "it25101520/reset-password";
        }
        if (password.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Password must be at least 6 characters.");
            return "it25101520/reset-password";
        }

        boolean success = authService.resetPassword(token, password);
        if (success) {
            return "redirect:/login?resetSuccess=true";
        } else {
            model.addAttribute("errorMessage", "Reset link is invalid or expired. Please submit another request.");
            return "it25101520/forgot-password";
        }
    }
}
