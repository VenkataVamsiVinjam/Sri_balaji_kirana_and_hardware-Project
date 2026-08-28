package com.sribalaji.erp.controller;

import com.sribalaji.erp.entity.Role;
import com.sribalaji.erp.entity.RoleName;
import com.sribalaji.erp.entity.User;
import com.sribalaji.erp.repository.RoleRepository;
import com.sribalaji.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("roles", RoleName.values());
        return "users/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String username, @RequestParam String password,
                        @RequestParam String fullName, @RequestParam String phone,
                        @RequestParam RoleName roleName, RedirectAttributes redirectAttributes) {

        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists.");
            return "redirect:/users/new";
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "User created successfully.");
        return "redirect:/users";
    }

    @PostMapping("/toggle/{id}")
    public String toggleEnabled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User status updated.");
        return "redirect:/users";
    }
}
