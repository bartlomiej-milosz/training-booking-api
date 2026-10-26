package dev.bartmilo.boxingacademy;

import org.springframework.boot.SpringApplication;

public class TestBoxingAcademyApplication {

  public static void main(String[] args) {
    SpringApplication.from(BoxingAcademyApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
