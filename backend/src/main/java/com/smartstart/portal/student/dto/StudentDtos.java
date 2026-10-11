package com.smartstart.portal.student.dto;

import com.smartstart.portal.student.entity.StudentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StudentDtos {

    /** Digits with optional leading +, spaces, dashes, dots and parentheses; 7-20 characters. */
    public static final String PHONE_REGEX = "^\\+?[0-9][0-9 ().-]{5,18}[0-9]$";
    public static final String PHONE_MESSAGE = "must be a valid phone number";

    private StudentDtos() {
    }

    public record CreateStudentRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Past(message = "must be in the past") LocalDate dateOfBirth,
            @NotBlank @Size(max = 30) String grade,
            @Size(max = 150) String school,
            List<@NotBlank @Size(max = 60) String> subjects,
            @Size(max = 2000) String address,
            @NotBlank @Size(max = 100) String guardianName,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String primaryContact,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String alternateContact,
            @Email(message = "must be a valid email") @Size(max = 150) String email,
            Boolean smsOptIn,
            @Size(max = 100) String emergencyContactName,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String emergencyContactPhone,
            @PastOrPresent(message = "cannot be in the future") LocalDate enrollmentDate,
            @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal perClassRate,
            StudentStatus status,
            Long classGroupId,
            @Size(max = 4000) String notes
    ) implements StudentFields {
    }

    public record UpdateStudentRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Past(message = "must be in the past") LocalDate dateOfBirth,
            @NotBlank @Size(max = 30) String grade,
            @Size(max = 150) String school,
            List<@NotBlank @Size(max = 60) String> subjects,
            @Size(max = 2000) String address,
            @NotBlank @Size(max = 100) String guardianName,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String primaryContact,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String alternateContact,
            @Email(message = "must be a valid email") @Size(max = 150) String email,
            Boolean smsOptIn,
            @Size(max = 100) String emergencyContactName,
            @Pattern(regexp = "^$|" + PHONE_REGEX, message = PHONE_MESSAGE) String emergencyContactPhone,
            LocalDate enrollmentDate,
            @NotNull @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal perClassRate,
            @NotNull StudentStatus status,
            Long classGroupId,
            @Size(max = 4000) String notes
    ) implements StudentFields {
    }

    /** Fields shared by create and update, so normalisation is written once. */
    public interface StudentFields {
        String fullName();

        String grade();

        List<String> subjects();

        String email();

        String primaryContact();

        String alternateContact();

        String emergencyContactName();

        String emergencyContactPhone();

        Long classGroupId();
    }

    public record UpdateStudentStatusRequest(@NotNull StudentStatus status) {
    }

    public record StudentResponse(
            Long id,
            String studentCode,
            String fullName,
            LocalDate dateOfBirth,
            String grade,
            String school,
            List<String> subjects,
            String address,
            String guardianName,
            String primaryContact,
            String alternateContact,
            String email,
            boolean smsOptIn,
            String emergencyContactName,
            String emergencyContactPhone,
            LocalDate enrollmentDate,
            BigDecimal perClassRate,
            StudentStatus status,
            Long classGroupId,
            String classGroupName,
            String notes,
            Boolean hasHistory,
            Instant createdAt,
            Instant updatedAt
    ) {
        public StudentResponse withHasHistory(boolean value) {
            return new StudentResponse(id, studentCode, fullName, dateOfBirth, grade, school, subjects, address,
                    guardianName, primaryContact, alternateContact, email, smsOptIn, emergencyContactName, emergencyContactPhone,
                    enrollmentDate, perClassRate, status, classGroupId, classGroupName, notes, value, createdAt, updatedAt);
        }
    }

    public record StudentFilter(String search, StudentStatus status, String grade, Long groupId) {
    }
}
