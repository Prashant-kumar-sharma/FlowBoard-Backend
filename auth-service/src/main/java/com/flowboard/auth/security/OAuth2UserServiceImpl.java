package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        processOAuth2User(registrationId, oAuth2User);
        return oAuth2User;
    }

    private void processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = null;
        String name = null;
        String providerId = null;
        User.AuthProvider provider = null;
        String avatarUrl = null;
        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");
            avatarUrl = (String) attributes.get("picture");
            provider = User.AuthProvider.GOOGLE;
        } else if ("github".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            if (name == null) name = (String) attributes.get("login");
            providerId = String.valueOf(attributes.get("id"));
            avatarUrl = (String) attributes.get("avatar_url");
            provider = User.AuthProvider.GITHUB;
        } else {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        if (email == null) {
            log.warn("Email not available from OAuth2 provider {}", registrationId);
            return;
        }

        final String finalEmail = email;
        final String finalName = name;
        final String finalProviderId = providerId;
        final User.AuthProvider finalProvider = provider;
        final String finalAvatarUrl = avatarUrl;

        userRepository.findByEmail(email).ifPresentOrElse(
            existingUser -> {
                boolean changed = false;
                if (existingUser.getProvider() == User.AuthProvider.LOCAL) {
                    existingUser.setProvider(finalProvider);
                    existingUser.setProviderId(finalProviderId);
                    changed = true;
                }
                // Sync avatar if current is empty
                if (existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) {
                    existingUser.setAvatarUrl(finalAvatarUrl);
                    changed = true;
                }
                if (changed) userRepository.save(existingUser);
            },
            () -> {
                String username = generateUsername(finalEmail);
                User newUser = User.builder()
                        .fullName(finalName != null ? finalName : finalEmail.split("@")[0])
                        .email(finalEmail)
                        .username(username)
                        .avatarUrl(finalAvatarUrl)
                        .provider(finalProvider)
                        .providerId(finalProviderId)
                        .isActive(true)
                        .build();
                userRepository.save(newUser);
                log.info("Created new user via OAuth2: {}", finalEmail);
            }
        );
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + suffix++;
        }
        return username;
    }
}
