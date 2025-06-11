package com.mbs.pessoas.model;

import lombok.Data;

@Data
public class Pessoa {
    private Long id;
    private String nome;
    private String cpf;
    private String cep;
    private String estado;
    private String cidade;
}