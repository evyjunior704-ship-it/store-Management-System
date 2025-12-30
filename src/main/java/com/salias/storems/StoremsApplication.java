package com.salias.storems;

import com.salias.storems.dataaccess.EmployeeRepository;
import com.salias.storems.model.Employee;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class StoremsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoremsApplication.class, args);
    }
    @Bean
    CommandLineRunner commandLineRunner(EmployeeRepository employeeRepository){
        return args -> {
            Employee maria = new Employee("Lebron","James", LocalDate.of(2007, 1,22),"79 Birmingham St","Store Manager", 2L);
            employeeRepository.save(maria);
        };
    }
}
