package com.anonymous.repository;

import com.anonymous.entity.ShiftChecklist;
import com.anonymous.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IShiftChecklistRepository extends JpaRepository<ShiftChecklist, String> {

    Optional<ShiftChecklist> findByUserAndWorkDateAndShiftCode(User user, LocalDate workDate, String shiftCode);

    @Query("SELECT sc FROM ShiftChecklist sc JOIN FETCH sc.user u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.position WHERE sc.workDate = :workDate AND sc.shiftCode = :shiftCode")
    List<ShiftChecklist> findByWorkDateAndShiftCode(@Param("workDate") LocalDate workDate, @Param("shiftCode") String shiftCode);

}
