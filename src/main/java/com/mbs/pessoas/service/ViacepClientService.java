package com.mbs.pessoas.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ViacepClientService {

    @Value("${spring.integrations.viacep.url}")
    private String viaCep;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("rawtypes")
    public Map buscarCep(String cep) {
        return restTemplate.getForObject(this.viaCep, Map.class, cep);
    }
}