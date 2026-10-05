package com.example.codetickets;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

@Configuration
public class JobConfiguration {

    @Autowired
    private PlatformTransactionManager transactionManager;

   @Bean
    public Job job(Step passoInicial, JobRepository jobRepository){
        return new JobBuilder("geracao-tickets",jobRepository)
                .start(passoInicial)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step passoInicial(ItemReader<Importacao> reader, ItemWriter<Importacao> writer,JobRepository jobRepository){
       return new StepBuilder("passo-inical",jobRepository)
               .<Importacao,Importacao>chunk(200)
               .transactionManager(transactionManager)
               .reader(reader)
               .writer(writer)
               .build();
    }

    @Bean
   public ItemReader<Importacao> reader(){
       return new FlatFileItemReaderBuilder<Importacao>()
               .name("leitura-csv")
               .resource(new FileSystemResource("files/dados.csv"))
               .comments("--")
               .delimited()
               .delimiter(";")
               .names("cpf","cliente","nascimento","evento","data","tipoIngresso","valor")
               .targetType(Importacao.class)
               .build();

   }

   @Bean
   public ItemWriter<Importacao> writer (DataSource dataSource){
       return new JdbcBatchItemWriterBuilder<Importacao>()
               .dataSource(dataSource)
               .sql(
                       "INSERT INTP importacao (id, cpf,cliente,evento,data,tipo_ingresso,valor,horario_importacao) VALUES" +
                       "(:id, :cpf, :cliente, : evento, :data, :tipo_ingresso, :valor, " + LocalDateTime.now() + ")"
               )
               .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
               .build();

   }
}
