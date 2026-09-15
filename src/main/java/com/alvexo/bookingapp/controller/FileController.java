package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.model.StoredFile;
import com.alvexo.bookingapp.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Files", description = "Streams uploaded workshop files (images, ID proofs, support attachments) by ID.")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Download a stored file", description = "Streams file content by ID. Requires authentication.")
    @GetMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        StoredFile metadata = fileStorageService.getMetadata(fileId);
        Resource resource = fileStorageService.loadAsResource(metadata);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }
}
