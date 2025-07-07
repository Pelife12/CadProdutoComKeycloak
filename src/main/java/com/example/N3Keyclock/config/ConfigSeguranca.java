package com.example.N3Keyclock.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfigSeguranca extends VaadinWebSecurity {

    public ConfigSeguranca() {
        super();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/images/**", "/line-awesome/**").permitAll()
                );

        super.configure(http);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.servicoUsuarioOidc())
                                .userAuthoritiesMapper(this.mapeadorAutoridadesUsuario())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .addLogoutHandler((requisicao, resposta, autenticacao) -> {
                            String uriEmissor = "http://localhost:8080/realms/meu-app-realm";
                            String idCliente = "frontend-produtos";
                            String uriRedirecionamentoPosLogout = requisicao.getScheme() + "://" + requisicao.getServerName() + ":" + requisicao.getServerPort() + "/";
                            String urlLogoutKeycloak = UriComponentsBuilder.fromUriString(uriEmissor)
                                    .pathSegment("protocol", "openid-connect", "logout")
                                    .queryParam("post_logout_redirect_uri", uriRedirecionamentoPosLogout)
                                    .queryParam("client_id", idCliente)
                                    .toUriString();
                            try {
                                resposta.sendRedirect(urlLogoutKeycloak);
                            } catch (IOException e) {
                                throw new RuntimeException("Erro ao redirecionar para logout do Keycloak", e);
                            }
                        })
                );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacaoJwt()))
        );
    }

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> conversorAutenticacaoJwt() {
        return new ConversorAutenticacaoJwtKeycloak();
    }

    @Bean
    public GrantedAuthoritiesMapper mapeadorAutoridadesUsuario() {
        return (autoridades) -> {
            Set<GrantedAuthority> autoridadesMapeadas = new HashSet<>();
            autoridades.forEach(autoridade -> {
                autoridadesMapeadas.add(autoridade);

                if (autoridade instanceof OidcUserAuthority) {
                    OidcUserAuthority autoridadeUsuarioOidc = (OidcUserAuthority) autoridade;
                    Map<String, Object> claims = autoridadeUsuarioOidc.getAttributes();

                    if (claims != null && claims.containsKey("resource_access")) {
                        Map<String, Object> acessoRecurso = (Map<String, Object>) claims.get("resource_access");
                        if (acessoRecurso != null && acessoRecurso.containsKey("frontend-produtos")) {
                            Map<String, Object> acessoCliente = (Map<String, Object>) acessoRecurso.get("frontend-produtos");
                            if (acessoCliente != null && acessoCliente.containsKey("roles")) {
                                List<String> roles = (List<String>) acessoCliente.get("roles");
                                autoridadesMapeadas.addAll(roles.stream()
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                        .collect(Collectors.toSet()));
                            }
                        }
                    }
                }
            });
            return autoridadesMapeadas;
        };
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> servicoUsuarioOidc() {
        final OidcUserService delegado = new OidcUserService();

        return (requisicaoUsuario) -> {
            OidcUser usuarioOidc = delegado.loadUser(requisicaoUsuario);

            String valorAccessToken = requisicaoUsuario.getAccessToken().getTokenValue();

            String uriEmissor = "http://localhost:8080/realms/meu-app-realm";

            JwtDecoder decodificadorJwt = NimbusJwtDecoder.withJwkSetUri(uriEmissor + "/protocol/openid-connect/certs").build();
            Jwt accessTokenJwt = decodificadorJwt.decode(valorAccessToken);

            Set<GrantedAuthority> autoridadesMapeadas = new HashSet<>(usuarioOidc.getAuthorities());

            Map<String, Object> acessoRecurso = accessTokenJwt.getClaimAsMap("resource_access");
            if (acessoRecurso != null && acessoRecurso.containsKey("frontend-produtos")) {
                Map<String, Object> acessoCliente = (Map<String, Object>) acessoRecurso.get("frontend-produtos");
                if (acessoCliente != null && acessoCliente.containsKey("roles")) {
                    List<String> roles = (List<String>) acessoCliente.get("roles");
                    autoridadesMapeadas.addAll(roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toSet()));
                }
            }

            return new UsuarioOidcComAutoridadesCustomizadas(usuarioOidc, autoridadesMapeadas);
        };
    }
}

class UsuarioOidcComAutoridadesCustomizadas implements OidcUser {
    private final OidcUser usuarioOidc;
    private final Set<GrantedAuthority> autoridades;

    public UsuarioOidcComAutoridadesCustomizadas(OidcUser usuarioOidc, Set<GrantedAuthority> autoridades) {
        this.usuarioOidc = usuarioOidc;
        this.autoridades = autoridades;
    }

    @Override
    public Map<String, Object> getClaims() {
        return usuarioOidc.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return usuarioOidc.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return usuarioOidc.getIdToken();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.autoridades;
    }

    @Override
    public String getName() {
        return usuarioOidc.getName();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return usuarioOidc.getAttributes();
    }
}