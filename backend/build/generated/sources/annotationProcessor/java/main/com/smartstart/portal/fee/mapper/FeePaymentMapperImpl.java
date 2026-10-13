package com.smartstart.portal.fee.mapper;

import com.smartstart.portal.fee.dto.FeeDtos;
import com.smartstart.portal.fee.entity.FeePayment;
import com.smartstart.portal.fee.entity.MonthlyFee;
import com.smartstart.portal.fee.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-12T12:49:59-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class FeePaymentMapperImpl implements FeePaymentMapper {

    @Override
    public FeeDtos.PaymentResponse toResponse(FeePayment payment) {
        if ( payment == null ) {
            return null;
        }

        Long monthlyFeeId = null;
        Long id = null;
        BigDecimal amount = null;
        LocalDate paymentDate = null;
        PaymentMethod method = null;
        String reference = null;
        Instant createdAt = null;

        monthlyFeeId = paymentMonthlyFeeId( payment );
        id = payment.getId();
        amount = payment.getAmount();
        paymentDate = payment.getPaymentDate();
        method = payment.getMethod();
        reference = payment.getReference();
        createdAt = payment.getCreatedAt();

        FeeDtos.PaymentResponse paymentResponse = new FeeDtos.PaymentResponse( id, monthlyFeeId, amount, paymentDate, method, reference, createdAt );

        return paymentResponse;
    }

    private Long paymentMonthlyFeeId(FeePayment feePayment) {
        MonthlyFee monthlyFee = feePayment.getMonthlyFee();
        if ( monthlyFee == null ) {
            return null;
        }
        return monthlyFee.getId();
    }
}
