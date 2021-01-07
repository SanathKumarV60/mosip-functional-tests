package io.mosip.test.packetcreator.mosippacketcreator;

import org.mosip.dataprovider.util.DataProviderConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})

public class MosipPacketCreatorApplication {

	
	public static void main(String[] args) {
		System.setProperty("spring.devtools.restart.enabled", "false");
		//		#security.ignored=/**
	//	System.setProperty("security.basic.enabled", "false");
	//	System.setProperty("management.security.enabled", "false");
	//	System.setProperty("security.ignored", "/**");
	
	
	
		
		SpringApplication.run(MosipPacketCreatorApplication.class, args);
	}
	/*
	@Bean
    public SecurityWebFilterChain chain(ServerHttpSecurity http, AuthenticationWebFilter webFilter) {
        return http.authorizeExchange().anyExchange().permitAll().and()
                .csrf().disable()
                .build();
	}*/
	
	 

}
