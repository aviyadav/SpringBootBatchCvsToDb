package com.sample.spring.boot.batch.export.config;

import com.sample.spring.boot.batch.export.model.Person;
import com.sample.spring.boot.batch.export.processor.PersonItemProcessor;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Bean
    public JdbcCursorItemReader<Person> reader() {
        JdbcCursorItemReader<Person> cursorItemReader = new JdbcCursorItemReader<>();
        cursorItemReader.setDataSource(dataSource);
        cursorItemReader.setSql("SELECT person_id, first_name, last_name, email, age FROM person");
        cursorItemReader.setRowMapper(new PersonRowMapper());
        return cursorItemReader;
    }

    @Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }

    @Bean
    public FlatFileItemWriter<Person> writer() {
        FlatFileItemWriter<Person> writer = new FlatFileItemWriter<>();
        writer.setResource(new ClassPathResource("persons.csv"));

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");

        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"personId", "firstName", "lastName", "email", "age"});
        lineAggregator.setFieldExtractor(fieldExtractor);

        writer.setLineAggregator(lineAggregator);
        return writer;
    }

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1").<Person, Person>chunk(100).reader(reader()).processor(processor()).writer(writer()).build();
    }

    @Bean
    public Job exportPerosnJob() {
        return jobBuilderFactory.get("exportPerosnJob").incrementer(new RunIdIncrementer()).flow(step1()).end().build();
    }
}
