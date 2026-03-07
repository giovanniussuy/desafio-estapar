package com.estapar.parking;

import org.springframework.boot.SpringApplication;

public class TestDesafioEstaparApplication {

	public static void main(String[] args) {
		SpringApplication.from(DesafioEstaparApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
