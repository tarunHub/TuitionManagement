package com.smartstart.portal.fee.mapper;

import com.smartstart.portal.fee.dto.FeeDtos.PaymentResponse;
import com.smartstart.portal.fee.entity.FeePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FeePaymentMapper {

    @Mapping(target = "monthlyFeeId", source = "monthlyFee.id")
    PaymentResponse toResponse(FeePayment payment);
}
