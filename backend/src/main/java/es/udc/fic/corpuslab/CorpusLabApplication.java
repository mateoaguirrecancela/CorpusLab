package es.udc.fic.corpuslab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CorpusLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorpusLabApplication.class, args);
	}

	@Bean
	@Profile("test")
	CacheManager testCacheManager() {
		return new ConcurrentMapCacheManager("projectMetrics", "projectProgress");
	}

}
