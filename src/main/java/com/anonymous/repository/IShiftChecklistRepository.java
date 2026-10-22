package com.anonymous.repository;

import com.anonymous.entity.ShiftChecklist;
import com.anonymous.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface IShiftChecklistRepository extends JpaRepository<ShiftChecklist, String> {

    Optional<ShiftChecklist> findByUserAndWorkDateAndShiftCode(User user, LocalDate workDate, String shiftCode);

}
