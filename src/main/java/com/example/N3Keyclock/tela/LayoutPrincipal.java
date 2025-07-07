package com.example.N3Keyclock.tela;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class LayoutPrincipal extends AppLayout {

    private final AuthenticationContext contextoAutenticacao;

    public LayoutPrincipal(AuthenticationContext contextoAutenticacao) {
        this.contextoAutenticacao = contextoAutenticacao;
        criarCabecalho();
    }

    private void criarCabecalho() {
        H1 logo = new H1("Sistema de Produtos");
        logo.addClassNames("text-l", "m-m");

        Button botaoLogout = new Button("Sair", e -> {
            contextoAutenticacao.logout();
        });

        HorizontalLayout cabecalho = new HorizontalLayout(logo);
        cabecalho.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        cabecalho.setWidth("100%");
        cabecalho.addClassNames("py-0", "px-m");

        contextoAutenticacao.<OidcUser>getAuthenticatedUser(OidcUser.class)
                .ifPresent(usuario -> {
                    String nomeUsuario = usuario.getPreferredUsername();
                    Button infoUsuario = new Button("Bem-vindo, " + nomeUsuario);
                    infoUsuario.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                    cabecalho.add(infoUsuario);
                });

        cabecalho.add(botaoLogout);
        addToNavbar(cabecalho);
    }
}