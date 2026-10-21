package com.anonymous.repository;

import com.anonymous.entity.ShiftChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IShiftChecklistRepository extends JpaRepository<ShiftChecklist, String> {

    Optional<ShiftChecklist> findByUsernameAndWorkDateAndShiftCode(String username, LocalDate workDate, String shiftCode);

    List<ShiftChecklist> findByWorkDate(LocalDate workDate);

    List<ShiftChecklist> findByUsernameAndWorkDate(String username, LocalDate workDate);

}
