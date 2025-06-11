package com.mbs.pessoas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PessoasApplication {

	public static void main(String[] args) {
		SpringApplication.run(PessoasApplication.class, args);
	}

}
