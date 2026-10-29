package com.example.pravin_quotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PravinQuotationApplication {

	public static void main(String[] args) {
		SpringApplication.run(PravinQuotationApplication.class, args);
	}

}
