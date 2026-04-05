package com.weathercody.api.service;

import com.weathercody.api.dto.SocialAuthRequest;
import com.weathercody.api.dto.SocialAuthResponse;
import com.weathercody.api.entity.SocialAccount;
import com.weathercody.api.entity.User;
import com.weathercody.api.repository.SocialAccountRepository;
import com.weathercody.api.repository.UserRepository;
import com.weathercody.api.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("social auth logs in when a social account already exists")
    void socialAuthenticate_returnsLoginForExistingSocialAccount() {
        User user = User.builder()
                .email("social@example.com")
                .name("Social User")
                .gender("F")
                .birthDate(LocalDate.of(2000, 1, 1))
                .phone("010-1111-2222")
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUserId("google-123")
                .email("social@example.com")
                .build();

        given(socialAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-123"))
                .willReturn(Optional.of(socialAccount));
        given(jwtTokenProvider.createToken("social@example.com")).willReturn("jwt-token");

        SocialAuthRequest request = new SocialAuthRequest();
        request.setProvider("google");
        request.setProviderUserId("google-123");

        SocialAuthResponse response = authService.socialAuthenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("social@example.com");
        assertThat(response.isProfileCompleted()).isTrue();
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("social auth creates a user and marks profile incomplete when optional fields are missing")
    void socialAuthenticate_createsUserWhenProfileFieldsAreMissing() {
        given(socialAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("social@example.com")).willReturn(false);
        given(jwtTokenProvider.createToken("social@example.com")).willReturn("social-token");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return user;
        });

        SocialAuthRequest request = new SocialAuthRequest();
        request.setProvider("GOOGLE");
        request.setProviderUserId("google-123");
        request.setEmail("social@example.com");
        request.setName("Social User");

        SocialAuthResponse response = authService.socialAuthenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("social-token");
        assertThat(response.isProfileCompleted()).isFalse();
        verify(userRepository).save(any(User.class));
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    @DisplayName("social auth creates a new user when profile fields are complete")
    void socialAuthenticate_createsUserWhenFieldsAreComplete() {
        given(socialAccountRepository.findByProviderAndProviderUserId("KAKAO", "kakao-123"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("social@example.com")).willReturn(false);
        given(userRepository.existsByPhone("010-1234-5678")).willReturn(false);
        given(jwtTokenProvider.createToken("social@example.com")).willReturn("new-social-token");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        });

        SocialAuthRequest request = new SocialAuthRequest();
        request.setProvider("KAKAO");
        request.setProviderUserId("kakao-123");
        request.setEmail("social@example.com");
        request.setName("Social User");
        request.setGender("F");
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        request.setPhone("010-1234-5678");
        request.setHeightCm((short) 165);
        request.setWeightKg((short) 55);
        request.setFootSizeMm((short) 240);

        SocialAuthResponse response = authService.socialAuthenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("new-social-token");
        assertThat(response.getUserId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.isProfileCompleted()).isTrue();

        ArgumentCaptor<SocialAccount> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(socialAccountCaptor.capture());
        assertThat(socialAccountCaptor.getValue().getProvider()).isEqualTo("KAKAO");
        assertThat(socialAccountCaptor.getValue().getProviderUserId()).isEqualTo("kakao-123");
    }

    @Test
    @DisplayName("social auth does not auto-link an existing email account")
    void socialAuthenticate_throwsWhenEmailAlreadyExists() {
        given(socialAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

        SocialAuthRequest request = new SocialAuthRequest();
        request.setProvider("GOOGLE");
        request.setProviderUserId("google-123");
        request.setEmail("existing@example.com");
        request.setName("Existing User");

        assertThatThrownBy(() -> authService.socialAuthenticate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 가입된 이메일입니다. 기존 로그인을 이용해주세요.");

        verify(userRepository, never()).save(any(User.class));
        verify(socialAccountRepository, never()).save(any(SocialAccount.class));
    }
}
