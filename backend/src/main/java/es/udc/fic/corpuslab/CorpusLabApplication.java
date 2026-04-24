package es.udc.fic.corpuslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CorpusLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorpusLabApplication.class, args);
	}

}
