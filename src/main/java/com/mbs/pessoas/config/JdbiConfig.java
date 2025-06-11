package com.mbs.pessoas.config;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mbs.pessoas.repository.PessoaRepository;

@Configuration
public class JdbiConfig {
    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new org.jdbi.v3.sqlobject.SqlObjectPlugin());
        return jdbi;
    }

    @Bean
    public PessoaRepository pessoaRepository(Jdbi jdbi) {
        return jdbi.onDemand(PessoaRepository.class);
    }
}