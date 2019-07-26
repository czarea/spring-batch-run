package com.czarea.springbatch.run.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zhouzx
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job1() {
        return jobBuilderFactory.get("job1")
                .incrementer(new RunIdIncrementer())
                .start(step1()).build();
    }

    private TaskletStep step1() {
        Tasklet tasklet = (contribution, context) -> {
            logger.info("This is from tasklet step with parameter ->"
                    + context.getStepContext().getJobParameters().get("message"));
            return RepeatStatus.FINISHED;
        };
        return stepBuilderFactory.get("step1").tasklet(tasklet).build();
    }

    @Bean
    public Job job2() {
        return jobBuilderFactory.get("job2")
                .incrementer(new RunIdIncrementer())
                .start(step2()).build();
    }

    @Bean
    public Step step2() {
        return stepBuilderFactory.get("step2")
                .<Map<String,String>,Map<String,String>>chunk(10)
                .reader(reader(null))
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Map<String,String>> reader(@Value("#{jobParameters['file']}") String file) {
        FlatFileItemReader<Map<String,String>> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource(file));
        reader.setStrict(false);

        DefaultLineMapper<Map<String,String>> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(":");
        tokenizer.setNames("key", "value");

        lineMapper.setFieldSetMapper((fieldSet) -> {
            Map<String,String> map = new LinkedHashMap<>();
            map.put(fieldSet.readString("key"),fieldSet.readString("value"));
            return map;
        });
        lineMapper.setLineTokenizer(tokenizer);
        reader.setLineMapper(lineMapper);

        return reader;
    }

    @Bean
    public ItemWriter<Map<String,String>> writer(){
        return (items) -> items.forEach(item -> {
            item.entrySet().forEach(entry -> {
                logger.info("key->[" + entry.getKey() + "] Value ->[" + entry.getValue() + "]");
            });
        });
    }
}
