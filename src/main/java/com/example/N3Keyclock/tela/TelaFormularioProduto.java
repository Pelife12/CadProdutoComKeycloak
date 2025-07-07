package com.example.N3Keyclock.tela;

import com.example.N3Keyclock.modelo.Produto;
import com.example.N3Keyclock.servico.ProdutoServico;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

@Route(value = "", layout = LayoutPrincipal.class)
@PageTitle("Cadastro de Produtos")
@PermitAll
public class TelaFormularioProduto extends VerticalLayout {

    private final ProdutoServico produtoServico;
    private final AuthenticationContext contextoAutenticacao;

    private TextField campoNome;
    private TextField campoDescricao;
    private NumberField campoPreco;
    private NumberField campoDesconto;

    private Button botaoSalvar;
    private Button botaoAtualizar;
    private Button botaoExcluir;
    private Button botaoLimpar;
    private Grid<Produto> gradeProdutos;

    private Binder<Produto> binder = new BeanValidationBinder<>(Produto.class); //

    @Autowired
    public TelaFormularioProduto(ProdutoServico produtoServico, AuthenticationContext contextoAutenticacao) {
        this.produtoServico = produtoServico;
        this.contextoAutenticacao = contextoAutenticacao;

        setSizeFull();
        setAlignItems(Alignment.CENTER);

        add(new H2("Cadastro de Produtos"));

        configurarFormulario();
        configurarGrade();
        configurarAutorizacao();
        listarProdutos();
    }

    private void configurarFormulario() {
        campoNome = new TextField("Nome");
        campoDescricao = new TextField("Descrição");
        campoPreco = new NumberField("Preço");
        campoPreco.setSuffixComponent(new com.vaadin.flow.component.html.Span("R$"));
        campoDesconto = new NumberField("Desconto (%)");
        campoDesconto.setSuffixComponent(new com.vaadin.flow.component.html.Span("%"));

        FormLayout layoutFormulario = new FormLayout();
        layoutFormulario.add(campoNome, campoDescricao, campoPreco, campoDesconto);
        layoutFormulario.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        binder.forField(campoNome).bind(Produto::getNome, Produto::setNome);
        binder.forField(campoDescricao).bind(Produto::getDescricao, Produto::setDescricao);
        binder.forField(campoPreco).bind(Produto::getPreco, Produto::setPreco);
        binder.forField(campoDesconto).bind(Produto::getDesconto, Produto::setDesconto);

        botaoSalvar = new Button("Salvar");
        botaoSalvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        botaoSalvar.addClickListener(event -> salvarProduto());

        botaoAtualizar = new Button("Atualizar");
        botaoAtualizar.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        botaoAtualizar.addClickListener(event -> atualizarProduto());
        botaoAtualizar.setEnabled(false);

        botaoExcluir = new Button("Excluir");
        botaoExcluir.addThemeVariants(ButtonVariant.LUMO_ERROR);
        botaoExcluir.addClickListener(event -> excluirProduto());
        botaoExcluir.setEnabled(false);

        botaoLimpar = new Button("Limpar");
        botaoLimpar.addClickListener(event -> limparFormulario());

        HorizontalLayout layoutBotoes = new HorizontalLayout(botaoSalvar, botaoAtualizar, botaoExcluir, botaoLimpar);

        add(layoutFormulario, layoutBotoes);
    }

    private void configurarGrade() {
        gradeProdutos = new Grid<>(Produto.class);
        gradeProdutos.setColumns("nome", "descricao", "preco", "desconto");
        gradeProdutos.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editarProduto(event.getValue());
                botaoAtualizar.setEnabled(true);
                botaoExcluir.setEnabled(true);
                botaoSalvar.setEnabled(false);
            } else {
                limparFormulario();
                botaoAtualizar.setEnabled(false);
                botaoExcluir.setEnabled(false);
                botaoSalvar.setEnabled(true);
            }
        });
        add(gradeProdutos);
    }

    private void configurarAutorizacao() {
        Collection<? extends GrantedAuthority> autoridades = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        boolean podeLer = autoridades.stream().anyMatch(a -> a.getAuthority().equals("ROLE_LER_PRODUTOS"));
        boolean podeCriar = autoridades.stream().anyMatch(a -> a.getAuthority().equals("ROLE_CRIAR_PRODUTO"));
        boolean podeAtualizar = autoridades.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ATUALIZAR_PRODUTO"));
        boolean podeExcluir = autoridades.stream().anyMatch(a -> a.getAuthority().equals("ROLE_EXCLUIR_PRODUTO"));

        gradeProdutos.setEnabled(podeLer);
        botaoSalvar.setVisible(podeCriar);
        botaoAtualizar.setVisible(podeAtualizar);
        botaoExcluir.setVisible(podeExcluir);

        if (!podeLer) {
            Notification.show("Você não tem permissão para visualizar produtos.");
        }
    }

    private void listarProdutos() {
        try {
            gradeProdutos.setItems(produtoServico.obterTodosProdutos());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            Notification.show("Você não tem permissão para listar produtos.");
            gradeProdutos.setItems(); // Limpa a grade se não houver permissão
        } catch (Exception e) {
            Notification.show("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    private void salvarProduto() {
        Produto produto = new Produto();
        if (binder.writeBeanIfValid(produto)) {
            try {
                if (produtoServico.criarProduto(produto) != null) {
                    Notification.show("Produto salvo com sucesso!");
                    limparFormulario();
                    listarProdutos();
                } else {
                    Notification.show("Erro ao salvar produto. Verifique os dados.");
                }
            } catch (org.springframework.security.access.AccessDeniedException e) {
                Notification.show("Você não tem permissão para criar produtos.");
            } catch (Exception e) {
                Notification.show("Erro ao salvar produto: " + e.getMessage());
            }
        }
    }

    private void atualizarProduto() {
        Produto produtoSelecionado = gradeProdutos.asSingleSelect().getValue();
        if (produtoSelecionado != null) {
            if (binder.writeBeanIfValid(produtoSelecionado)) {
                try {
                    if (produtoServico.atualizarProduto(produtoSelecionado.getId(), produtoSelecionado) != null) {
                        Notification.show("Produto atualizado com sucesso!");
                        limparFormulario();
                        listarProdutos();
                    } else {
                        Notification.show("Erro ao atualizar produto. Produto não encontrado ou dados inválidos.");
                    }
                } catch (org.springframework.security.access.AccessDeniedException e) {
                    Notification.show("Você não tem permissão para atualizar produtos.");
                } catch (Exception e) {
                    Notification.show("Erro ao atualizar produto: " + e.getMessage());
                }
            }
        }
    }

    private void excluirProduto() {
        Produto produtoSelecionado = gradeProdutos.asSingleSelect().getValue();
        if (produtoSelecionado != null) {
            try {
                if (produtoServico.excluirProduto(produtoSelecionado.getId())) {
                    Notification.show("Produto excluído com sucesso!");
                    limparFormulario();
                    listarProdutos();
                } else {
                    Notification.show("Erro ao excluir produto. Produto não encontrado.");
                }
            } catch (org.springframework.security.access.AccessDeniedException e) {
                Notification.show("Você não tem permissão para excluir produtos.");
            } catch (Exception e) {
                Notification.show("Erro ao excluir produto: " + e.getMessage());
            }
        }
    }

    private void editarProduto(Produto produto) {
        binder.readBean(produto);
    }

    private void limparFormulario() {
        binder.readBean(new Produto()); // Reseta o formulário
        gradeProdutos.asSingleSelect().clear();
        botaoAtualizar.setEnabled(false);
        botaoExcluir.setEnabled(false);
        botaoSalvar.setEnabled(true);
    }
}