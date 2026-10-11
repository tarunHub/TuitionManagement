package com.smartstart.portal.fee.controller;

import com.smartstart.portal.common.util.FileResponses;
import com.smartstart.portal.fee.dto.FeeDtos.CreatePaymentRequest;
import com.smartstart.portal.fee.dto.FeeDtos.FeeListResponse;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.fee.dto.FeeDtos.GenerateFeesResponse;
import com.smartstart.portal.fee.dto.FeeDtos.UpdateMonthlyFeeRequest;
import com.smartstart.portal.fee.dto.FeeDtos.UpdatePaymentRequest;
import com.smartstart.portal.fee.entity.FeeStatus;
import com.smartstart.portal.fee.service.FeeExportService;
import com.smartstart.portal.fee.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Fees")
@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService service;
    private final FeeExportService exportService;

    @Operation(summary = "Fee rows for a month with derived paid/balance/status and totals")
    @GetMapping
    public FeeListResponse list(@Parameter(example = "2026-08") @RequestParam YearMonth month,
                                @RequestParam(required = false) FeeStatus status,
                                @RequestParam(required = false) Long groupId) {
        return service.list(month, status, groupId);
    }

    @Operation(summary = "Idempotently generate/refresh fees for every active student")
    @PostMapping("/generate")
    public GenerateFeesResponse generate(@Parameter(example = "2026-08") @RequestParam YearMonth month) {
        return service.generate(month);
    }

    @Operation(summary = "Update classes scheduled, notes or the rate snapshot")
    @PutMapping("/{id}")
    public FeeRowResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMonthlyFeeRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Record a payment")
    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeRowResponse addPayment(@PathVariable Long id, @Valid @RequestBody CreatePaymentRequest request) {
        return service.addPayment(id, request);
    }

    @PutMapping("/{id}/payments/{paymentId}")
    public FeeRowResponse updatePayment(@PathVariable Long id, @PathVariable Long paymentId,
                                        @Valid @RequestBody UpdatePaymentRequest request) {
        return service.updatePayment(id, paymentId, request);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public FeeRowResponse deletePayment(@PathVariable Long id, @PathVariable Long paymentId) {
        return service.deletePayment(id, paymentId);
    }

    @Operation(summary = "Export the month's fees to Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Parameter(example = "2026-08") @RequestParam YearMonth month,
                                         @RequestParam(required = false) FeeStatus status,
                                         @RequestParam(required = false) Long groupId) {
        return FileResponses.excel(exportService.export(month, status, groupId), "fees-" + month + ".xlsx");
    }

    @Operation(summary = "Fee history for one student")
    @GetMapping("/students/{studentId}")
    public List<FeeRowResponse> studentHistory(@PathVariable Long studentId) {
        return service.studentHistory(studentId);
    }
}
