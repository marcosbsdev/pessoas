package com.mbs.pessoas.repository;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.springframework.stereotype.Repository;

import com.mbs.pessoas.model.Pessoa;

@Repository
public interface PessoaRepository {
     @SqlQuery(
          """
               SELECT 
                    * 
               FROM 
                    pessoa 
               WHERE 
                    cep = :cep
          """)
     @RegisterBeanMapper(Pessoa.class)
     List<Pessoa> findByCep(String cep);

     @GetGeneratedKeys
     @SqlUpdate("INSERT INTO pessoa (nome, cpf, cep, cidade, estado) VALUES (:nome, :cpf, :cep, null, null)")
     @RegisterBeanMapper(Pessoa.class)
     Long save(@BindBean Pessoa pessoa);

     @SqlUpdate("UPDATE pessoa SET nome = :nome, cpf = :cpf, cep = :cep WHERE id = :id")
     @RegisterBeanMapper(Pessoa.class)
     Integer update(Long id, @BindBean Pessoa pessoa);

     @SqlUpdate("UPDATE pessoa SET cidade = :cidade, estado = :estado WHERE id = :id")
     @RegisterBeanMapper(Pessoa.class)
     Integer updateCidadeEstado(Long id, @BindBean Pessoa pessoa);

     @SqlQuery("SELECT * FROM pessoa")
     @RegisterBeanMapper(Pessoa.class)
     List<Pessoa> findAll();

     @SqlQuery("SELECT * FROM pessoa WHERE id = :id")
     @RegisterBeanMapper(Pessoa.class)
     Optional<Pessoa> findById(Long id);

     @SqlUpdate("DELETE FROM pessoa WHERE id = :id")
     void deleteById(Long id);
}