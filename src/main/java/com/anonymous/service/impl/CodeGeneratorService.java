package com.anonymous.service.impl;

import com.anonymous.entity.CodeSequence;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICodeSequenceRepository;
import com.anonymous.service.ICodeGeneratorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CodeGeneratorService implements ICodeGeneratorService {

    ICodeSequenceRepository codeSequenceRepository;

    // Joins the caller's transaction, so a rolled-back insert also gives its number back
    @Override
    @Transactional
    public String next(String sequenceName) {
        CodeSequence sequence = codeSequenceRepository.findByNameForUpdate(sequenceName)
                .orElseThrow(() -> new AppException(ErrorCode.CODE_SEQUENCE_NOT_EXIST));
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        codeSequenceRepository.save(sequence);
        return sequence.getPrefix() + String.format("%0" + sequence.getPadding() + "d", value);
    }

}
