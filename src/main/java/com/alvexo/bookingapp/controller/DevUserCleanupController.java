package com.alvexo.bookingapp.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.UserSummaryDto;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.service.DevUserCleanupService;
import com.alvexo.bookingapp.util.MobileNumberUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ⚠️  DEV / TEST ONLY — completely absent from the application context when the
 * "prod" Spring profile is active (@Profile("!prod")).
 *
 * <p>Use this controller to clean up test data without logging into the database.
 * Remove this class (or ensure "prod" profile is set) before going live.
 *
 * <p>All endpoints live under {@code /api/dev/users}.
 *
 * <h3>Safety guard on bulk-delete-all</h3>
 * {@code DELETE /api/dev/users/all} requires the header
 * {@code X-Confirm-Delete: DELETE_ALL_USERS} to prevent accidental wipes.
 */
@Tag(name = "Dev — User Cleanup (NON-PROD ONLY)", description = "Development and testing endpoints for deleting users without database access. NOT available in production.")
@RestController
@RequestMapping("/api/dev/users")
@Profile("!prod")
public class DevUserCleanupController {

    private static final String CONFIRM_ALL_HEADER     = "X-Confirm-Delete";
    private static final String CONFIRM_ALL_VALUE      = "DELETE_ALL_USERS";

    private final DevUserCleanupService cleanupService;

    public DevUserCleanupController(DevUserCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    // -------------------------------------------------------------------------
    // List endpoints — read-only, useful before deciding what to delete
    // -------------------------------------------------------------------------

    /**
     * GET /api/dev/users
     * Lists all users in the table (id, email, mobile, role, active, createdAt).
     */
    @Operation(summary = "List all users", description = "Returns all users with id, email, mobile, role, and createdAt. Use before deleting to confirm targets.")
    @GetMapping
    public ResponseEntity<MyApiResponse<List<UserSummaryDto>>> listAllUsers() {
        List<UserSummaryDto> users = cleanupService.listAllUsers();
        return ResponseEntity.ok(
                MyApiResponse.success("Found " + users.size() + " user(s)", users));
    }

    /**
     * GET /api/dev/users/by-role?role=VEHICLE_USER
     * Lists users filtered by role.
     */
    @Operation(summary = "List users by role", description = "Filters users by role: VEHICLE_USER, MECHANIC, SALES_REPRESENTATIVE, or ADMINISTRATOR.")
    @GetMapping("/by-role")
    public ResponseEntity<MyApiResponse<List<UserSummaryDto>>> listByRole(
            @RequestParam UserRole role) {
        List<UserSummaryDto> users = cleanupService.listUsersByRole(role);
        return ResponseEntity.ok(
                MyApiResponse.success("Found " + users.size() + " user(s) with role " + role, users));
    }

    // -------------------------------------------------------------------------
    // Single-user delete
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/dev/users/{id}
     * Deletes one user by their database ID, along with all their child records.
     */
    @Operation(summary = "Delete user by ID", description = "Deletes a user and all their FK-dependent child records by database ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<MyApiResponse<UserSummaryDto>> deleteById(
            @PathVariable Long id) {
        UserSummaryDto deleted = cleanupService.deleteUserById(id);
        return ResponseEntity.ok(
                MyApiResponse.success("User deleted successfully", deleted));
    }

    /**
     * DELETE /api/dev/users/by-email?email=user@example.com
     * Deletes one user by email, along with all their child records.
     */
    @Operation(summary = "Delete user by email", description = "Deletes a user and all their child records by email address.")
    @DeleteMapping("/by-email")
    public ResponseEntity<MyApiResponse<UserSummaryDto>> deleteByEmail(
            @RequestParam String email) {
        UserSummaryDto deleted = cleanupService.deleteUserByEmail(email);
        return ResponseEntity.ok(
                MyApiResponse.success("User deleted successfully", deleted));
    }

    /**
     * DELETE /api/dev/users/by-mobile?mobile=9876543210
     * Deletes one user by mobile number.
     * Accepts any format (+91XXXXXXXXXX, 91XXXXXXXXXX, XXXXXXXXXX) —
     * the number is normalised before the lookup.
     */
    @Operation(summary = "Delete user by mobile", description = "Deletes a user by mobile number. Accepts +91XXXXXXXXXX, 91XXXXXXXXXX, or plain XXXXXXXXXX formats.")
    @DeleteMapping("/by-mobile")
    public ResponseEntity<MyApiResponse<UserSummaryDto>> deleteByMobile(
            @RequestParam String mobile) {
        String normalised = MobileNumberUtil.normalize(mobile);
        UserSummaryDto deleted = cleanupService.deleteUserByMobile(normalised);
        return ResponseEntity.ok(
                MyApiResponse.success("User deleted successfully", deleted));
    }

    // -------------------------------------------------------------------------
    // Bulk delete by role
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/dev/users/by-role?role=VEHICLE_USER
     * Deletes all users with a specific role and their child records.
     */
    @Operation(summary = "Delete all users by role", description = "Deletes every user with the specified role and their child records.")
    @DeleteMapping("/by-role")
    public ResponseEntity<MyApiResponse<String>> deleteByRole(
            @RequestParam UserRole role) {
        int count = cleanupService.deleteAllUsersByRole(role);
        return ResponseEntity.ok(
                MyApiResponse.success("Deleted " + count + " user(s) with role " + role, null));
    }

    // -------------------------------------------------------------------------
    // Nuke everything — requires explicit confirmation header
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/dev/users/all
     * Deletes ALL users and every dependent record.
     *
     * <p><b>Requires header:</b> {@code X-Confirm-Delete: DELETE_ALL_USERS}
     * <p>Returns HTTP 400 if the header is missing or incorrect.
     */
    @Operation(summary = "Delete ALL users", description = "Deletes every row in the users table. Requires header X-Confirm-Delete: DELETE_ALL_USERS to prevent accidents.")
    @DeleteMapping("/all")
    public ResponseEntity<MyApiResponse<String>> deleteAll(
            @RequestHeader(value = CONFIRM_ALL_HEADER, required = false) String confirm) {

        if (!CONFIRM_ALL_VALUE.equals(confirm)) {
            return ResponseEntity.badRequest().body(
                    MyApiResponse.error(
                            "Missing or incorrect confirmation header. " +
                            "Add header: " + CONFIRM_ALL_HEADER + ": " + CONFIRM_ALL_VALUE));
        }

        int count = cleanupService.deleteAllUsers();
        return ResponseEntity.ok(
                MyApiResponse.success("Deleted all " + count + " user(s) from the database", null));
    }
}
