package br.com.cloudkoodi.config;

import br.com.cloudkoodi.domain.User;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchJobConfig {

    @Value("${classpath:csv/user.csv}")
    private Resource resource;
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    public BatchJobConfig(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
    }

    @Bean
    public Job job(@Qualifier("csv-to-db") Step step) {
        return new JobBuilder("user-processing-job", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Step step() {
        return new StepBuilder("step-1", jobRepository)
                    .tasklet(new Tasklet() {

                        @Override
                        public RepeatStatus execute(StepContribution arg0, ChunkContext arg1) throws Exception {
                            System.out.println("Hello World");
                            return RepeatStatus.FINISHED;
                        }
                        
                    }, platformTransactionManager)
                .allowStartIfComplete(true)
                    .build();
    }

    @Bean("csv-to-db")
    public Step csvToDB(ItemReader<User> reader, ItemWriter<User> writer) {
        return new StepBuilder("step-csv-db", jobRepository)
                .<User, User>chunk(10, platformTransactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public FlatFileItemReader<User> reader() {
        FlatFileItemReaderBuilder<User> builder = new FlatFileItemReaderBuilder<>();

        builder.linesToSkip(1);
        builder.resource(resource);
        builder.lineMapper(lineMapper());
        builder.saveState(false);

        return builder.build();
    }

    @Bean
    public JdbcBatchItemWriter<User> writer(DataSource dataSource) {
        JdbcBatchItemWriterBuilder<User> builder = new JdbcBatchItemWriterBuilder<>();
        builder.dataSource(dataSource);
        builder.sql("INSERT INTO tb_usuarios (usuario_id,first_name,last_name,age,email) " +
                "VALUES (:id,:firstName,:lastName,:age,:email)");
        builder.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());

        return builder.build();
    }

    private LineMapper<User> lineMapper() {
        DefaultLineMapper<User> lineMapper = new DefaultLineMapper<>();

        lineMapper.setLineTokenizer(tokenizer());
        lineMapper.setFieldSetMapper(fieldLineMapper());

        return lineMapper;
    }

    private FieldSetMapper<User> fieldLineMapper() {
        return fs -> {
            String id = fs.readString("id");
            String first = fs.readString("first_name");
            String last = fs.readString("last_name");
            int age = fs.readInt("age");
            String email = fs.readString("email");

            return new User(id,first,last,age,email);
        };
    }

    private LineTokenizer tokenizer() {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("id","first_name","last_name","age","email");
        return tokenizer;
    }
}
