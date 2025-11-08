package com.mbs.pessoas.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.mbs.pessoas.model.Pessoa;
import com.mbs.pessoas.model.PessoaRequest;
import com.mbs.pessoas.repository.PessoaRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class PessoaService {
    private final PessoaRepository repository;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    public static final String CACHE_PESSOAS = "pessoas";
    public static final String CACHE_PESSOAS_POR_CEP = "pessoasPorCep";

    @SuppressWarnings("null")
    public Pessoa salvar(PessoaRequest pessoa) {
        Pessoa salvarPessoa = new Pessoa();
        salvarPessoa.setId(pessoa.getId() != null ? pessoa.getId() : null);
        salvarPessoa.setNome(pessoa.getNome());
        salvarPessoa.setCpf(pessoa.getCpf());
        salvarPessoa.setCep(pessoa.getCep());

        // Pessoa existente
        if (salvarPessoa.getId() != null) {
            int row = repository.update(salvarPessoa.getId(), salvarPessoa);
            // Se a atualização falhar, lança uma exceção
            if (row == 0 || row == -1)
                throw new RuntimeException("Erro ao atualizar a pessoa com ID: " + salvarPessoa.getId());
        } else {
            // Salva nova pessoa e bbtem ID da Nova pessoa ao Salvar
            salvarPessoa.setId(repository.save(salvarPessoa));
        }

        // Atualiza o cache do CEP, se já existir
        this.atualizaCachePessoaPorCEP(salvarPessoa);
        // Atualiza o cache de todas as pessoas
        this.atualizaCacheTodasPessoas();

        // Publica na fila
        this.enviarParaFila(salvarPessoa);

        return salvarPessoa;
    }

    private void atualizaCacheTodasPessoas() {
        if (this.cacheExistente(CACHE_PESSOAS, "all")) {
            List<Pessoa> todasPessoas = repository.findAll();
            this.manutencaoCache("all", CACHE_PESSOAS, todasPessoas);
        }
    }

    @Cacheable(value = CACHE_PESSOAS, key = "'all'", unless = "#result == null or #result.isEmpty()")
    public List<Pessoa> listar() {
        return repository.findAll();
    }

    @Cacheable(value = CACHE_PESSOAS_POR_CEP, key = "#cep", unless = "#result == null or #result.isEmpty()")
    public List<Pessoa> listarPorCep(String cep) {
        return repository.findByCep(cep);
    }

    public Optional<Pessoa> buscarPorId(Long id) {
        return repository.findById(id);
    }

    private void atualizaCachePessoaPorCEP(Pessoa pessoa) {
        if (pessoa.getCep() != null && this.cacheExistente(CACHE_PESSOAS_POR_CEP, pessoa.getCep())) {
            List<Pessoa> pessoasPorCep = repository.findByCep(pessoa.getCep());
            this.manutencaoCache(pessoa.getCep(), CACHE_PESSOAS_POR_CEP, pessoasPorCep);
        }
    }

    public void deletar(Long id) {
        Optional<Pessoa> optionalPessoa = repository.findById(id);
        Pessoa pessoa = optionalPessoa.get();
        
        repository.deleteById(id);
        this.atualizaTodosCache(pessoa);
    }

    public void atualizaTodosCache(Pessoa pessoa) {
        this.atualizaCacheTodasPessoas();
        this.atualizaCachePessoaPorCEP(pessoa);
    }

    public Pessoa salvarCidadeEstado(Long id, String cidade, String estado) {
        Pessoa pessoa = repository.findById(id).orElse(null);
        if (pessoa != null) {
            pessoa.setCidade(cidade);
            pessoa.setEstado(estado);
            int row = repository.updateCidadeEstado(id, pessoa);
            if (row == 0 || row == -1) {
                log.warn("Não foi possivel atraulizar a cidade e estado para a pessoa com ID: {}", id);
            }
        }
        return pessoa;
    }

    @org.springframework.retry.annotation.Retryable(maxAttempts = 5, backoff = @org.springframework.retry.annotation.Backoff(delay = 2000, multiplier = 2))
    private void enviarParaFila(Pessoa pessoa) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", pessoa.getId());
        payload.put("cep", pessoa.getCep());
        // publish with retry/backoff in case Redis briefly unavailable
        redisTemplate.convertAndSend("filaCep", payload);
    }

    @org.springframework.retry.annotation.Recover
    private void recoverEnviarParaFila(RuntimeException e, Pessoa pessoa) {
        log.error("Failed to publish to Redis after retries. Message will be dropped for id={}. Cause: {}", pessoa.getId(), e.getMessage());
    }


    @SuppressWarnings("null")
    public void manutencaoCache(String key, String value, List<Pessoa> pessoasParaCache) {
        if (pessoasParaCache == null || pessoasParaCache.isEmpty() || cacheManager.getCache(value) == null)
            return;
        log.info("Iniciando manutenção do cache para key: {} e value: {}", key, value);
        cacheManager.getCache(value).put(key, pessoasParaCache);
        log.info("Manutenção do cache concluída.");
    }

    public boolean cacheExistente(String value, String key) {
        Cache cache = cacheManager.getCache(value);
        if (cache == null) return false;
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null && wrapper.get() != null;
    }

}