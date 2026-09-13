package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.ChangePasswordRequest;
import com.smartfinancialexpenseanalysis.dto.UpdateProfileRequest;
import com.smartfinancialexpenseanalysis.dto.UserProfileResponse;
import com.smartfinancialexpenseanalysis.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authenticated user profile view and security settings.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        UserProfileResponse response = profileService.getProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                             Authentication authentication) {
        UserProfileResponse response = profileService.updateProfile(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                      Authentication authentication) {
        ApiResponse response = profileService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
