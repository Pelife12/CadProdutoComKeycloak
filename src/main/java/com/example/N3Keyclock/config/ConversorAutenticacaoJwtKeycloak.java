package com.example.N3Keyclock.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConversorAutenticacaoJwtKeycloak implements Converter<Jwt, JwtAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter conversorAutoridadesJwt = new JwtGrantedAuthoritiesConverter();

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> autoridades = Stream.concat(
                conversorAutoridadesJwt.convert(jwt).stream(),
                extrairRolesDoResourceAccess(jwt, "frontend-produtos").stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, autoridades);
    }

    private Collection<? extends GrantedAuthority> extrairRolesDoResourceAccess(Jwt jwt, String idCliente) {
        Map<String, Object> acessoRecurso = jwt.getClaimAsMap("resource_access");
        if (acessoRecurso == null || !acessoRecurso.containsKey(idCliente)) {
            return new HashSet<>();
        }

        Map<String, Object> acessoCliente = (Map<String, Object>) acessoRecurso.get(idCliente);
        if (acessoCliente == null || !acessoCliente.containsKey("roles")) {
            return new HashSet<>();
        }

        List<String> roles = (List<String>) acessoCliente.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }
}