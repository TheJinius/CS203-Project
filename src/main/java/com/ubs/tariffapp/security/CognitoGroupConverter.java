package com.ubs.tariffapp.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class CognitoGroupConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object groups = jwt.getClaim("cognito:groups");
        
        if (groups instanceof List) {
            return ((List<?>) groups).stream()
                .map(Object::toString)
                .filter(g -> g.equals("Admins") || g.equals("Users"))
                .map(g -> new SimpleGrantedAuthority("ROLE_" + g))
                .collect(Collectors.toSet());
        }
        
        return Collections.emptySet();
    }
}
