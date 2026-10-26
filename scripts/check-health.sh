# !/bin/bash

# Check the health of the Spring Boot application
curl --fail http://127.0.0.1:8080/actuator/health
