package com.anonymous.repository;

import com.anonymous.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDistrictRepository extends JpaRepository<District, String> {

    List<District> findByCity_Id(String id);
    
}
