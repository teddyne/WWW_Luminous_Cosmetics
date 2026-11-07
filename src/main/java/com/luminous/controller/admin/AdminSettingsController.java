package com.luminous.controller.admin;

import com.luminous.dto.PasswordForm;
import com.luminous.dto.ProfileForm;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AdminSettingsController(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @GetMapping
    public String settings(@AuthenticationPrincipal CustomUserDetails admin, Model model) {
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(admin.getUser().getFullName());
            form.setPhone(admin.getUser().getPhone());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        model.addAttribute("activeMenu", "settings");
        return "admin/settings";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails admin,
                                Model model,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return settings(admin, model);
        }
        User user = users.findById(admin.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        String phone = form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim();
        if (phone != null && users.findByPhone(phone)
                .map(other -> !other.getId().equals(user.getId())).orElse(false)) {
            model.addAttribute("formError", "Số điện thoại đã được dùng bởi tài khoản khác");
            return settings(admin, model);
        }
        user.setFullName(form.getFullName().trim());
        user.setPhone(phone);
        users.save(user);
        redirect.addFlashAttribute("toast", "Đã cập nhật thông tin tài khoản");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/settings";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails admin,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = users.findById(admin.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if (!result.hasErrors() && !passwords.matches(form.getCurrentPassword(), user.getPassword())) {
            result.rejectValue("currentPassword", "mismatch", "Mật khẩu hiện tại không đúng");
        }
        if (!result.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
        if (result.hasErrors()) {
            return settings(admin, model);
        }
        user.setPassword(passwords.encode(form.getNewPassword()));
        users.save(user);
        redirect.addFlashAttribute("toast", "Đã đổi mật khẩu");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/settings";
    }
}
