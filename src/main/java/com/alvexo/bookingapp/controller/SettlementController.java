package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.SettlementQueryRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.SettlementFaqEntryResponse;
import com.alvexo.bookingapp.dto.response.SettlementQueryResponse;
import com.alvexo.bookingapp.dto.response.SettlementResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Settlement;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.SettlementService;
import com.alvexo.bookingapp.util.SettlementPdfGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Workshop Finance", description = "Settlement list/receipt, PDFs, FAQ, and settlement queries "
        + "(WORKSHOP_FINANCE_API_SPEC.md). Requires JWT with MECHANIC role — the settlement owner is always "
        + "resolved from the token.")
@RestController
@RequestMapping("/api/settlements")
@PreAuthorize("hasRole('MECHANIC')")
public class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepository;

    public SettlementController(SettlementService settlementService, UserRepository userRepository) {
        this.settlementService = settlementService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "List settlements",
               description = "Defaults to the 7 most recent settlements. Pass year+month for a calendar month "
                       + "(only the last 3 completed months are selectable), or from+to for a custom range "
                       + "(within the previous 30 days, span at most 7 days).")
    @GetMapping
    public ResponseEntity<MyApiResponse<List<SettlementResponse>>> getSettlements(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit,
            Authentication authentication) {
        List<SettlementResponse> response =
                settlementService.getSettlements(resolveUser(authentication), year, month, from, to, limit);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    @Operation(summary = "Get a settlement", description = "Full detail for one settlement owned by the caller.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<SettlementResponse>> getSettlementById(
            @PathVariable Long id, Authentication authentication) {
        SettlementResponse response = settlementService.getSettlementById(resolveUser(authentication), id);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    @Operation(summary = "Download a settlement receipt PDF",
               description = "Only available once the settlement's status is Paid.")
    @GetMapping("/{id}/receipt.pdf")
    public ResponseEntity<byte[]> getReceiptPdf(@PathVariable Long id, Authentication authentication) {
        User mechanic = resolveUser(authentication);
        Settlement settlement = settlementService.getPaidSettlementForReceipt(mechanic, id);
        byte[] pdf = SettlementPdfGenerator.generateReceipt(mechanic, settlement);
        return pdfResponse(pdf, "settlement-receipt-" + settlement.getSettlementDate() + ".pdf");
    }

    @Operation(summary = "Download a settlement statement PDF",
               description = "All settlements in the given period (same filters as the list endpoint).")
    @GetMapping("/statement.pdf")
    public ResponseEntity<byte[]> getStatementPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        List<Settlement> settlements = settlementService.getStatementSettlements(mechanic, year, month, from, to);
        LocalDate periodFrom = settlements.isEmpty() ? LocalDate.now() : settlements.get(settlements.size() - 1).getSettlementDate();
        LocalDate periodTo = settlements.isEmpty() ? LocalDate.now() : settlements.get(0).getSettlementDate();
        byte[] pdf = SettlementPdfGenerator.generateStatement(mechanic, settlements, periodFrom, periodTo);
        return pdfResponse(pdf, "settlement-statement-" + periodFrom + "-to-" + periodTo + ".pdf");
    }

    @Operation(summary = "Get the settlement calculation FAQ", description = "Server-driven copy for the "
            + "\"How is My Settlement Calculated?\" section.")
    @GetMapping("/faq")
    public ResponseEntity<MyApiResponse<List<SettlementFaqEntryResponse>>> getFaq() {
        return ResponseEntity.ok(MyApiResponse.success(settlementService.getFaq()));
    }

    @Operation(summary = "Raise a settlement query",
               description = "settlementDate must match one of the caller's own settlements.")
    @PostMapping("/queries")
    public ResponseEntity<MyApiResponse<SettlementQueryResponse>> submitQuery(
            @Valid @RequestBody SettlementQueryRequest request, Authentication authentication) {
        SettlementQueryResponse response = settlementService.submitQuery(resolveUser(authentication), request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(MyApiResponse.success("Query submitted", response));
    }

    @Operation(summary = "List my settlement queries")
    @GetMapping("/queries")
    public ResponseEntity<MyApiResponse<List<SettlementQueryResponse>>> getQueries(Authentication authentication) {
        List<SettlementQueryResponse> response = settlementService.getQueries(resolveUser(authentication));
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
