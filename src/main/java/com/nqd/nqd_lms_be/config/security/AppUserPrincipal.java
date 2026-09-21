package com.nqd.nqd_lms_be.config.security;

import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class AppUserPrincipal implements OAuth2User, OidcUser, Serializable {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private final String phoneNumber;
    private final UserStatus status;
    private final Set<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public AppUserPrincipal(User user, Set<String> roles, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatarUrl = user.getAvatarUrl();
        this.phoneNumber = user.getPhoneNumber();
        this.status = user.getStatus();
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        this.authorities = this.roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
        this.attributes = attributes != null ? Collections.unmodifiableMap(attributes) : Collections.emptyMap();
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public static AppUserPrincipal create(User user, Set<String> roles, Map<String, Object> attributes) {
        return new AppUserPrincipal(user, roles, attributes, null, null);
    }

    public static AppUserPrincipal create(User user, Set<String> roles, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        return new AppUserPrincipal(user, roles, attributes, idToken, userInfo);
    }

    public AppUserPrincipal withRoles(Set<String> newRoles) {
        User user = User.builder()
                .email(this.email)
                .fullName(this.fullName)
                .avatarUrl(this.avatarUrl)
                .phoneNumber(this.phoneNumber)
                .status(this.status)
                .build();
        user.setId(this.id);
        return new AppUserPrincipal(user, newRoles, this.attributes, this.idToken, this.userInfo);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return email != null ? email : (id != null ? id.toString() : "");
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
