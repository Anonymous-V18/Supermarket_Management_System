package com.anonymous.repository;

import com.anonymous.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IWardRepository extends JpaRepository<Ward, String> {

    List<Ward> findByDistrict_Id(String id);

}
