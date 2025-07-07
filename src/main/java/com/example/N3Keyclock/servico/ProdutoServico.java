package com.example.N3Keyclock.servico;

import com.example.N3Keyclock.modelo.Produto;
import com.example.N3Keyclock.repositorio.ProdutoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProdutoServico {

    @Autowired
    private ProdutoRepositorio produtoRepositorio;

    @PreAuthorize("hasRole('ROLE_LER_PRODUTOS')")
    public List<Produto> obterTodosProdutos() {
        return produtoRepositorio.findAll();
    }

    @PreAuthorize("hasRole('ROLE_LER_PRODUTOS')")
    public Optional<Produto> obterProdutoPorId(Long id) {
        return produtoRepositorio.findById(id);
    }

    @PreAuthorize("hasRole('ROLE_CRIAR_PRODUTO')")
    public Produto criarProduto(Produto produto) {
        return produtoRepositorio.save(produto);
    }

    @PreAuthorize("hasRole('ROLE_ATUALIZAR_PRODUTO')")
    public Produto atualizarProduto(Long id, Produto detalhesProduto) {
        Optional<Produto> produtoOpt = produtoRepositorio.findById(id);
        if (produtoOpt.isPresent()) {
            Produto produtoExistente = produtoOpt.get();
            produtoExistente.setNome(detalhesProduto.getNome());
            produtoExistente.setDescricao(detalhesProduto.getDescricao());
            produtoExistente.setPreco(detalhesProduto.getPreco());
            produtoExistente.setDesconto(detalhesProduto.getDesconto());
            return produtoRepositorio.save(produtoExistente);
        } else {
            return null;
        }
    }

    @PreAuthorize("hasRole('ROLE_EXCLUIR_PRODUTO')")
    public boolean excluirProduto(Long id) {
        if (produtoRepositorio.existsById(id)) {
            produtoRepositorio.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}