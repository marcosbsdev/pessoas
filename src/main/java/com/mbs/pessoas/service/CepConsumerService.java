package com.mbs.pessoas.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbs.pessoas.model.Pessoa;
import com.mbs.pessoas.repository.PessoaRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class CepConsumerService implements MessageListener {

    private final PessoaRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ViacepClientService viacepClientService;
    public static final String CACHE_PESSOAS_POR_CEP = "pessoasPorCep";
    private final PessoaService pessoaService;

    @SuppressWarnings({ "null", "unchecked", "rawtypes" })
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getBody(), Map.class);

            Object idObj = payload.get("id");

            Long id = switch (idObj) {
                case null -> throw new IllegalArgumentException("Campo id ausente no payload: " + payload);
                case Number n -> n.longValue();
                case String s -> Long.valueOf(s);
                case List<?> list when !list.isEmpty() -> Long.valueOf(String.valueOf(list.getLast()));
                default -> throw new IllegalArgumentException("Formato inesperado para o campo id: " + idObj);
            };

            String cep = payload.get("cep").toString();

            // Chama ViaCEP
            log.info("Buscando informações do CEP: {}", cep);
            Map resposta = viacepClientService.buscarCep(cep);

            if (Objects.nonNull(resposta.get("erro"))) {
                log.warn("Nenhuma informação encontrada para o CEP: {}", cep);
                return;
            }
            this.processarRespostaCep(resposta, id);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Erro ao processar mensagem: {}", message, e);
        }
    }

    public void processarRespostaCep(Map<String, Object> resposta, Long id) {
        Map<String, Object> dadosCep = resposta;
        log.info("Dados do CEP: {}", dadosCep);

        String cidade = dadosCep.getOrDefault("localidade", null) != null ? dadosCep.get("localidade").toString()
                : null;
        String estado = dadosCep.getOrDefault("uf", null) != null ? dadosCep.get("uf").toString() : null;

        Pessoa pessoa = pessoaService.salvarCidadeEstado(id, cidade, estado);
        log.info("Pessoa atualizada: {}", pessoa);

        // Atualiza o cache do CEP, se já existir
        this.atualizarCacheCep(pessoa);
    }

    public void atualizarCacheCep(Pessoa pessoa) {
        if (pessoa != null && pessoa.getCep() != null
                && pessoaService.cacheExistente(CACHE_PESSOAS_POR_CEP, pessoa.getCep())) {
            List<Pessoa> pessoasPorCep = repository.findByCep(pessoa.getCep());
            pessoaService.manutencaoCache(pessoa.getCep(), CACHE_PESSOAS_POR_CEP, pessoasPorCep);
        }
    }
}