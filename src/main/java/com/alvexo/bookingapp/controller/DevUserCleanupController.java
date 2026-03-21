package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.UserSummaryDto;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.service.DevUserCleanupService;
import com.alvexo.bookingapp.util.MobileNumberUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> listAllUsers() {
        List<UserSummaryDto> users = cleanupService.listAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Found " + users.size() + " user(s)", users));
    }

    /**
     * GET /api/dev/users/by-role?role=VEHICLE_USER
     * Lists users filtered by role.
     */
    @GetMapping("/by-role")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> listByRole(
            @RequestParam UserRole role) {
        List<UserSummaryDto> users = cleanupService.listUsersByRole(role);
        return ResponseEntity.ok(
                ApiResponse.success("Found " + users.size() + " user(s) with role " + role, users));
    }

    // -------------------------------------------------------------------------
    // Single-user delete
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/dev/users/{id}
     * Deletes one user by their database ID, along with all their child records.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummaryDto>> deleteById(
            @PathVariable Long id) {
        UserSummaryDto deleted = cleanupService.deleteUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully", deleted));
    }

    /**
     * DELETE /api/dev/users/by-email?email=user@example.com
     * Deletes one user by email, along with all their child records.
     */
    @DeleteMapping("/by-email")
    public ResponseEntity<ApiResponse<UserSummaryDto>> deleteByEmail(
            @RequestParam String email) {
        UserSummaryDto deleted = cleanupService.deleteUserByEmail(email);
        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully", deleted));
    }

    /**
     * DELETE /api/dev/users/by-mobile?mobile=9876543210
     * Deletes one user by mobile number.
     * Accepts any format (+91XXXXXXXXXX, 91XXXXXXXXXX, XXXXXXXXXX) —
     * the number is normalised before the lookup.
     */
    @DeleteMapping("/by-mobile")
    public ResponseEntity<ApiResponse<UserSummaryDto>> deleteByMobile(
            @RequestParam String mobile) {
        String normalised = MobileNumberUtil.normalize(mobile);
        UserSummaryDto deleted = cleanupService.deleteUserByMobile(normalised);
        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully", deleted));
    }

    // -------------------------------------------------------------------------
    // Bulk delete by role
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/dev/users/by-role?role=VEHICLE_USER
     * Deletes all users with a specific role and their child records.
     */
    @DeleteMapping("/by-role")
    public ResponseEntity<ApiResponse<String>> deleteByRole(
            @RequestParam UserRole role) {
        int count = cleanupService.deleteAllUsersByRole(role);
        return ResponseEntity.ok(
                ApiResponse.success("Deleted " + count + " user(s) with role " + role, null));
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
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> deleteAll(
            @RequestHeader(value = CONFIRM_ALL_HEADER, required = false) String confirm) {

        if (!CONFIRM_ALL_VALUE.equals(confirm)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            "Missing or incorrect confirmation header. " +
                            "Add header: " + CONFIRM_ALL_HEADER + ": " + CONFIRM_ALL_VALUE));
        }

        int count = cleanupService.deleteAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Deleted all " + count + " user(s) from the database", null));
    }
}
