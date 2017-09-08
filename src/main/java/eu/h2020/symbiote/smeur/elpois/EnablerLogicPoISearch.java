package eu.h2020.symbiote.smeur.elpois;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

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

}
