package com.salias.storems.model.emergencyinfo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyInfoRepository extends JpaRepository<EmergencyInfo, Long> {

    List<EmergencyInfo> findByEmployee_Id(Long employeeId);
}
