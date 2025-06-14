package com.mbs.pessoas.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ViacepClientService {

    @Value("${spring.integrations.viacep.url}")
    private String viaCep;

    private ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("rawtypes")
    public Map buscarCep(String cep) { 
        Map retorno;
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(this.viaCep.replace("{cep}", cep))).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            retorno = mapper.readValue(response.body(), Map.class);
        } catch (Exception e) {
            log.warn("Erro ao buscar CEP: {}", cep, e);
            retorno = Map.of("erro", "Erro ao buscar CEP: " + cep + ". Detalhes: " + e.getMessage());
        }
        return retorno;
    }
}