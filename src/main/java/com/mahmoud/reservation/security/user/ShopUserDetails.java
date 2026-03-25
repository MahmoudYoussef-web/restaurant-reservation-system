package com.mahmoud.reservation.security.user;

import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class ShopUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private boolean accountNonLocked;
    private boolean enabled;
    private Collection<? extends GrantedAuthority> authorities;

    public static ShopUserDetails from(User user) {
        return new ShopUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                true,
                true,
                user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .toList()
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}