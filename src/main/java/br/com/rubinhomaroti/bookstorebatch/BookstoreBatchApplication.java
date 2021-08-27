package br.com.rubinhomaroti.bookstorebatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;


@SpringBootApplication
@EnableBatchProcessing
public class BookstoreBatchApplication {

	// Logger para exibir as saídas durante o processamento do batch
	Logger logger = LoggerFactory.getLogger(BookstoreBatchApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BookstoreBatchApplication.class, args);
	}

	// A notação do @Bean notifica o Spring para adicionar os jobs,
	// steps e o próprio chunk em seu container de dependências

	// Um chunk é composto por um reader, um processor (opcional) e um writer.

	// Reader do chunk que será injetado no batch.
	// Rotina para ler um arquivo csv com dados de vários livros
	@Bean
	public FlatFileItemReader<Book> itemReader(@Value("${file.chunk}") Resource resource) {
		logger.info("Chunk item reader setup");
		return new FlatFileItemReaderBuilder<Book>()
				.name("Book csv reader")
				.targetType(Book.class)
				.resource(resource)
				.delimited().delimiter(";").names("title", "isbn")
				.build();
	}

	// Processor do chunk que será injetado no batch.
	// Nele são aplicadas as regras de negócios sobre os dados a serem importados
	@Bean
	public ItemProcessor<Book, Book> itemProcessor() {
		logger.info("Chunk item processor setup");
		return (Book) -> {
			Book.setTitle(Book.getTitle().toUpperCase());
			Book.setIsbn(Book.getIsbn().trim());
			return Book;
		};
	}

	// Writer do chunk que será injetado no batch.
	// Os dados serão inseridos no banco H1
	@Bean
	public JdbcBatchItemWriter<Book> itemWriter(DataSource dataSource) {
		logger.info("Chunk item writer setup");
		return new JdbcBatchItemWriterBuilder<Book>()
				.dataSource(dataSource)
				.sql("insert into TB_BOOKS (title, isbn) values (:title, :isbn)")
				.beanMapped()
				.build();
	}

	// Step do chunk a ser injetado no batch
	@Bean
	public Step step(StepBuilderFactory stepBuilderFactory,
					 ItemReader<Book> itemReader,
					 ItemProcessor<Book, Book> itemProcessor,
					 ItemWriter<Book> itemWriter) {
		logger.info("Chunk step setup");
		return stepBuilderFactory.get("Step chunk csv to jdbc")
				.<Book, Book>chunk(2)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.build();
	}

	// Job para executar a tasklet que será injetado no batch.
	@Bean
	public Job job(JobBuilderFactory jobBuilderFactory, Step step) {
		logger.info("Chunk job setup");
		return jobBuilderFactory.get("Chunk job")
				.start(step)
				.build();
	}
}
