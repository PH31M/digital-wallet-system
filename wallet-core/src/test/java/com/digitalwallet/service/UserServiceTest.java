package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.ChangePasswordRequest;
import com.digitalwallet.api.dto.request.UpdateProfileRequest;
import com.digitalwallet.api.dto.response.UserProfileResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void updateProfile_updatesFullNameAndPhoneNumber() {
        UserService userService = userService();
        User user = user("user@example.com", "Nguyen Van A", "old-hash");
        UpdateProfileRequest request = new UpdateProfileRequest(" Nguyen Van B ", "0909123456");

        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(user, request);

        assertThat(user.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(user.getPhoneNumber()).isEqualTo("0909123456");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(response.getPhoneNumber()).isEqualTo("0909123456");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_invalidCurrentPassword_throwsInvalidCurrentPassword() {
        UserService userService = userService();
        User user = user("user@example.com", "Nguyen Van A", "old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-password", "N3w@Password");

        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(user, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        assertThat(user.getTokenVersion()).isZero();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_validCurrentPassword_hashesNewPasswordAndIncrementsTokenVersion() {
        UserService userService = userService();
        User user = user("user@example.com", "Nguyen Van A", "old-hash");
        user.setTokenVersion(3);
        ChangePasswordRequest request = new ChangePasswordRequest("Old@Password1", "N3w@Password");

        when(passwordEncoder.matches("Old@Password1", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("N3w@Password")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(user, request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(4);
        verify(userRepository).save(user);
    }

    private UserService userService() {
        return new UserService(userRepository, passwordEncoder);
    }

    private User user(String email, String fullName, String passwordHash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setTokenVersion(0);
        return user;
    }
}
