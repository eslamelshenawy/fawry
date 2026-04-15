package com.fawry.routing.security;

import com.fawry.routing.domain.entity.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AppUserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final String billerCode;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public AppUserPrincipal(AppUser user) {
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.billerCode = user.getBillerCode();
        this.enabled = user.isEnabled();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public boolean isAccountNonExpired()   { return enabled; }
    @Override public boolean isAccountNonLocked()    { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return enabled; }
    @Override public boolean isEnabled()             { return enabled; }
}
