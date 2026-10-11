package com.smartstart.portal.student.mapper;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.student.dto.StudentDtos;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.entity.StudentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-06T20:17:38-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class StudentMapperImpl implements StudentMapper {

    @Override
    public StudentDtos.StudentResponse toResponse(Student student) {
        if ( student == null ) {
            return null;
        }

        Long classGroupId = null;
        String classGroupName = null;
        Long id = null;
        String studentCode = null;
        String fullName = null;
        LocalDate dateOfBirth = null;
        String grade = null;
        String school = null;
        List<String> subjects = null;
        String address = null;
        String guardianName = null;
        String primaryContact = null;
        String alternateContact = null;
        String email = null;
        boolean smsOptIn = false;
        String emergencyContactName = null;
        String emergencyContactPhone = null;
        LocalDate enrollmentDate = null;
        BigDecimal perClassRate = null;
        StudentStatus status = null;
        String notes = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        classGroupId = studentClassGroupId( student );
        classGroupName = studentClassGroupName( student );
        id = student.getId();
        studentCode = student.getStudentCode();
        fullName = student.getFullName();
        dateOfBirth = student.getDateOfBirth();
        grade = student.getGrade();
        school = student.getSchool();
        Set<String> set = student.getSubjects();
        if ( set != null ) {
            subjects = new ArrayList<String>( set );
        }
        address = student.getAddress();
        guardianName = student.getGuardianName();
        primaryContact = student.getPrimaryContact();
        alternateContact = student.getAlternateContact();
        email = student.getEmail();
        smsOptIn = student.isSmsOptIn();
        emergencyContactName = student.getEmergencyContactName();
        emergencyContactPhone = student.getEmergencyContactPhone();
        enrollmentDate = student.getEnrollmentDate();
        perClassRate = student.getPerClassRate();
        status = student.getStatus();
        notes = student.getNotes();
        createdAt = student.getCreatedAt();
        updatedAt = student.getUpdatedAt();

        Boolean hasHistory = null;

        StudentDtos.StudentResponse studentResponse = new StudentDtos.StudentResponse( id, studentCode, fullName, dateOfBirth, grade, school, subjects, address, guardianName, primaryContact, alternateContact, email, smsOptIn, emergencyContactName, emergencyContactPhone, enrollmentDate, perClassRate, status, classGroupId, classGroupName, notes, hasHistory, createdAt, updatedAt );

        return studentResponse;
    }

    @Override
    public Student toEntity(StudentDtos.CreateStudentRequest request) {
        if ( request == null ) {
            return null;
        }

        Student student = new Student();

        student.setFullName( request.fullName() );
        student.setDateOfBirth( request.dateOfBirth() );
        student.setGrade( request.grade() );
        student.setSchool( request.school() );
        student.setAddress( request.address() );
        student.setGuardianName( request.guardianName() );
        student.setPrimaryContact( request.primaryContact() );
        student.setAlternateContact( request.alternateContact() );
        student.setEmail( request.email() );
        if ( request.smsOptIn() != null ) {
            student.setSmsOptIn( request.smsOptIn() );
        }
        student.setEmergencyContactName( request.emergencyContactName() );
        student.setEmergencyContactPhone( request.emergencyContactPhone() );
        student.setEnrollmentDate( request.enrollmentDate() );
        student.setNotes( request.notes() );

        return student;
    }

    @Override
    public void update(StudentDtos.UpdateStudentRequest request, Student student) {
        if ( request == null ) {
            return;
        }

        student.setFullName( request.fullName() );
        student.setDateOfBirth( request.dateOfBirth() );
        student.setGrade( request.grade() );
        student.setSchool( request.school() );
        student.setAddress( request.address() );
        student.setGuardianName( request.guardianName() );
        student.setPrimaryContact( request.primaryContact() );
        student.setAlternateContact( request.alternateContact() );
        student.setEmail( request.email() );
        if ( request.smsOptIn() != null ) {
            student.setSmsOptIn( request.smsOptIn() );
        }
        student.setEmergencyContactName( request.emergencyContactName() );
        student.setEmergencyContactPhone( request.emergencyContactPhone() );
        student.setEnrollmentDate( request.enrollmentDate() );
        student.setPerClassRate( request.perClassRate() );
        student.setStatus( request.status() );
        student.setNotes( request.notes() );
    }

    private Long studentClassGroupId(Student student) {
        ClassGroup classGroup = student.getClassGroup();
        if ( classGroup == null ) {
            return null;
        }
        return classGroup.getId();
    }

    private String studentClassGroupName(Student student) {
        ClassGroup classGroup = student.getClassGroup();
        if ( classGroup == null ) {
            return null;
        }
        return classGroup.getName();
    }
}
