package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Maps FlowBoard actors to Spring Security roles:
 *
 *   GUEST          -> handled by permitAll() in SecurityConfig (no UserDetails needed)
 *   MEMBER         -> ROLE_MEMBER
 *   BOARD_ADMIN    -> ROLE_BOARD_ADMIN
 *   PLATFORM_ADMIN -> ROLE_PLATFORM_ADMIN  (also carries ROLE_BOARD_ADMIN + ROLE_MEMBER)
 *   SYSTEM         -> ROLE_SYSTEM
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";
    private static final String ROLE_BOARD_ADMIN = "ROLE_BOARD_ADMIN";
    private static final String ROLE_MEMBER = "ROLE_MEMBER";
    private static final String ROLE_SYSTEM = "ROLE_SYSTEM";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<SimpleGrantedAuthority> authorities = buildAuthorities(user.getRole());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .accountLocked(!user.getIsActive())
                .disabled(!user.getIsActive())
                .build();
    }

    /**
     * PLATFORM_ADMIN inherits BOARD_ADMIN and MEMBER privileges.
     * BOARD_ADMIN inherits MEMBER privileges.
     * This allows @PreAuthorize("hasRole('MEMBER')") to pass for admins too.
     */
    private List<SimpleGrantedAuthority> buildAuthorities(User.Role role) {
        return switch (role) {
            case PLATFORM_ADMIN -> List.of(
                    new SimpleGrantedAuthority(ROLE_PLATFORM_ADMIN),
                    new SimpleGrantedAuthority(ROLE_BOARD_ADMIN),
                    new SimpleGrantedAuthority(ROLE_MEMBER)
            );
            case BOARD_ADMIN -> List.of(
                    new SimpleGrantedAuthority(ROLE_BOARD_ADMIN),
                    new SimpleGrantedAuthority(ROLE_MEMBER)
            );
            case SYSTEM -> List.of(new SimpleGrantedAuthority(ROLE_SYSTEM));
            default -> List.of(new SimpleGrantedAuthority(ROLE_MEMBER));
        };
    }
}
