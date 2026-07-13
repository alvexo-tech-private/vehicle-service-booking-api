package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.StoredFile;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on local disk under {@code app.upload.dir} and tracks
 * metadata/ownership in {@link StoredFile}. Swap the disk calls for an object-store
 * SDK (S3, GCS, etc.) if this ever needs to run across multiple app instances.
 */
@Service
public class FileStorageService {

    private final StoredFileRepository storedFileRepository;
    private final Path uploadRoot;

    public FileStorageService(StoredFileRepository storedFileRepository,
                               @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.storedFileRepository = storedFileRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not initialise upload directory: " + uploadRoot, e);
        }
    }

    @Transactional
    public StoredFile store(MultipartFile file, User owner, long maxSizeBytes, Set<String> allowedContentTypes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("VALIDATION_ERROR", "file is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessRuleException("IMAGE_TOO_LARGE",
                    "File exceeds the maximum allowed size of " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }
        if (allowedContentTypes != null && !allowedContentTypes.contains(file.getContentType())) {
            throw new BusinessRuleException("IMAGE_BAD_FORMAT",
                    "Unsupported file type: " + file.getContentType());
        }

        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;
        Path destination = uploadRoot.resolve(storedName).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new BusinessRuleException("VALIDATION_ERROR", "Invalid file name");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }

        StoredFile storedFile = StoredFile.builder()
                .owner(owner)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storagePath(storedName)
                .build();
        return storedFileRepository.save(storedFile);
    }

    @Transactional
    public void delete(StoredFile storedFile) {
        try {
            Files.deleteIfExists(uploadRoot.resolve(storedFile.getStoragePath()).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete stored file", e);
        }
        storedFileRepository.delete(storedFile);
    }

    public StoredFile getMetadata(Long fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    public Resource loadAsResource(StoredFile storedFile) {
        try {
            Path path = uploadRoot.resolve(storedFile.getStoragePath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File content not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File content not found");
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
