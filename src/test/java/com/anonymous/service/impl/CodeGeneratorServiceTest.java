package com.anonymous.service.impl;

import com.anonymous.entity.CodeSequence;
import com.anonymous.exception.AppException;
import com.anonymous.repository.ICodeSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeGeneratorServiceTest {

    @Mock
    ICodeSequenceRepository codeSequenceRepository;

    @InjectMocks
    CodeGeneratorService codeGeneratorService;

    @Test
    void codesArePrefixedZeroPaddedAndIncrementing() {
        CodeSequence sequence = CodeSequence.builder().name("EMPLOYEE").prefix("NV").padding(5).nextValue(7L).build();
        when(codeSequenceRepository.findByNameForUpdate("EMPLOYEE")).thenReturn(Optional.of(sequence));

        assertThat(codeGeneratorService.next("EMPLOYEE")).isEqualTo("NV00007");
        assertThat(codeGeneratorService.next("EMPLOYEE")).isEqualTo("NV00008");
        assertThat(sequence.getNextValue()).isEqualTo(9L);
    }

    @Test
    void unknownSequenceFails() {
        when(codeSequenceRepository.findByNameForUpdate("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codeGeneratorService.next("NOPE")).isInstanceOf(AppException.class);
    }

}
