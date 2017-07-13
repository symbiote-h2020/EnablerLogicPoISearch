package eu.h2020.symbiote.smeur.elpois;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.smeur.elpois.messaging.RabbitManager;

/**
 * @author PetarKrivic (13.7.2017)
 *
 *         Main entry point to start spring boot application.
 */
@EnableDiscoveryClient    
@EnableAutoConfiguration
@SpringBootApplication
public class EnablerLogicPoISearch {

	private static Log log = LogFactory.getLog(EnablerLogicPoISearch.class);
	
	public static void main(String[] args) {
		SpringApplication.run(EnablerLogicPoISearch.class, args);
    }
	
	@Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;

        @Autowired
        public CLR(RabbitManager rabbitManager) {
            this.rabbitManager = rabbitManager;
        }

        @Override
        public void run(String... args) throws Exception {

            //message retrieval - start rabbit exchange and consumers
            this.rabbitManager.init();
            log.info("CLR run() and Rabbit Manager init()");
        }
    }
}
