package dev.bartmilo.trainingbooking;

import org.springframework.boot.SpringApplication;

public class TestTrainingBookingApplication {

  public static void main(String[] args) {
    SpringApplication.from(TrainingBookingApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
