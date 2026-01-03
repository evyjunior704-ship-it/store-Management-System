package com.salias.storems;

import com.salias.storems.model.employee.EmployeeRepository;
import com.salias.storems.model.store.StoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StoremsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoremsApplication.class, args);
    }
    @Bean
    CommandLineRunner commandLineRunner(StoreRepository storeRepository,EmployeeRepository employeeRepository){
        return args -> {
           /* Store store1 = new Store("store A",
                    "50 Berwick St");
            Store store2 = new Store("Store B",
                    "30 Brampton Ave");

            Employee emp1 = new Employee("Evy Junior",
                    "Kembou Dongmo",
                    LocalDate.of(2007,01,22),
                    "79 Cargo Rd",
                    "cashier"
                    ,store1);

            Employee emp2 = new Employee("John",
                    "Givings",
                    LocalDate.of(1992,04,16),
                    "80 Fulton Rd",
                    "store Manager"
                    ,store2);
            Employee emp3 = new Employee("Daniel",
                    "McFarlane",
                    LocalDate.of(1997,01,05),
                    "35 Sunset Rd",
                    "Store associate"
                    ,store1);
            storeRepository.save(store1);
            storeRepository.save(store2);

            employeeRepository.save(emp1);
            employeeRepository.save(emp2);
            employeeRepository.save(emp3);*/
        };
    }
}
