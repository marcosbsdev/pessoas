package com.mbs.pessoas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mbs.pessoas.model.Pessoa;
import com.mbs.pessoas.model.PessoaRequest;
import com.mbs.pessoas.service.PessoaService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/pessoas")
@AllArgsConstructor
public class PessoaController {

    private final PessoaService service;

    @PostMapping
    public Pessoa criar(@RequestBody @Valid PessoaRequest pessoa) {
        return service.salvar(pessoa);
    }

    @GetMapping
    public List<Pessoa> listar() {
        return service.listar();
    }

    @GetMapping("/cep/{cep}")
    public List<Pessoa> listarPorCep(@PathVariable String cep) {
        return service.listarPorCep(cep);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pessoa> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pessoa> atualizar(@PathVariable Long id, @RequestBody @Valid PessoaRequest pessoa) {
        return service.buscarPorId(id)
                .map(p -> {
                    pessoa.setId(id);
                    return ResponseEntity.ok(service.salvar(pessoa));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (service.buscarPorId(id).isPresent()) {
            service.deletar(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}