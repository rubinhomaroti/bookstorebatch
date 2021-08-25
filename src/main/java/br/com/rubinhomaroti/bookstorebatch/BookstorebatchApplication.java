package br.com.rubinhomaroti.bookstorebatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.nio.file.Paths;

@SpringBootApplication
@EnableBatchProcessing
public class BookstorebatchApplication {

	// Logger para exibir as saídas durante o processamento do batch
	Logger logger = LoggerFactory.getLogger(BookstorebatchApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BookstorebatchApplication.class, args);
	}

	// A notação do @Bean notifica o Spring para adicionar os jobs,
	// steps e o próprio tasklet em seu container de dependências

	// Tasklet do job que será injetado no batch.
	// Rotina simples para apagar um arquivo que será definido nos resources
	@Bean
	public Tasklet tasklet(@Value("${file.path}") String filePath) {
		return ((contribution, chunkContext) -> {
			try {
				File file = Paths.get(filePath).toFile();
				if (file.delete()) {
					logger.info("File deleted successfully");
				} else {
					logger.warn("It was no possible to delete the specified file");
				}
			} catch (Exception e) {
				logger.error("Error during processing file: " + e.getMessage());
			}

			return RepeatStatus.FINISHED;
		});
	}

	// Step do job que será injetado no batch.
	@Bean
	public Step step(StepBuilderFactory stepBuilderFactory, Tasklet tasklet) {
		return stepBuilderFactory.get("Delete file step")
				.tasklet(tasklet)
				.allowStartIfComplete(true)
				.build();
	}

	// Job para executar a tasklet que será injetado no batch.
	@Bean
	public Job job(JobBuilderFactory jobBuilderFactory, Step step) {
		return jobBuilderFactory.get("Delete file job")
				.start(step)
				.build();
	}
}
