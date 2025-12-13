package capstone_project.controller.order;

import capstone_project.dtos.response.order.transaction.StaffTransactionResponse;
import capstone_project.service.services.order.transaction.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${transaction.api.base-path}")
@Tag(name = "Transaction Management", description = "APIs for managing transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/staff/list")
    @Operation(summary = "Get all transactions for staff", description = "Retrieve all transactions with minimal contract info, sorted by newest first")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<List<StaffTransactionResponse>> getAllTransactionsForStaff() {
        List<StaffTransactionResponse> transactions = transactionService.getAllTransactionsForStaff();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/staff/{transactionId}")
    @Operation(summary = "Get transaction detail for staff", description = "Retrieve transaction details with full contract information")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<StaffTransactionResponse> getTransactionDetailForStaff(
            @PathVariable UUID transactionId) {
        StaffTransactionResponse transaction = transactionService.getTransactionDetailForStaff(transactionId);
        return ResponseEntity.ok(transaction);
    }
}
