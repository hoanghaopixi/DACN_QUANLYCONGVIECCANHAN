package com.taskmanager.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public final class UploadedMediaValidator {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 25L * 1024 * 1024;

    private UploadedMediaValidator() {}

    public record DetectedMedia(String extension, String type) {}

    public static DetectedMedia detectImage(MultipartFile file) throws IOException {
        requireFile(file, MAX_IMAGE_BYTES);
        byte[] header = readHeader(file);
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) return new DetectedMedia(".jpg", "IMAGE");
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return new DetectedMedia(".png", "IMAGE");
        if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) return new DetectedMedia(".gif", "IMAGE");
        if (header.length >= 12 && startsWithAscii(header, "RIFF") && asciiAt(header, 8, "WEBP")) {
            return new DetectedMedia(".webp", "IMAGE");
        }
        throw new IllegalArgumentException("Tệp không phải ảnh JPEG, PNG, GIF hoặc WebP hợp lệ.");
    }

    public static DetectedMedia detectBackground(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không được để trống.");
        }
        byte[] header = readHeader(file);
        if (file.getSize() <= MAX_IMAGE_BYTES) {
            try {
                return detectImage(file);
            } catch (IllegalArgumentException ignored) {
                // Continue to detect supported video containers.
            }
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Video không được vượt quá 25 MB.");
        }
        if (header.length >= 8 && asciiAt(header, 4, "ftyp")) return new DetectedMedia(".mp4", "VIDEO");
        if (startsWith(header, 0x1A, 0x45, 0xDF, 0xA3)) return new DetectedMedia(".webm", "VIDEO");
        if (startsWithAscii(header, "OggS")) return new DetectedMedia(".ogg", "VIDEO");
        throw new IllegalArgumentException("Chỉ nhận ảnh JPEG, PNG, GIF, WebP hoặc video MP4, WebM, Ogg hợp lệ.");
    }

    private static void requireFile(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không được để trống.");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 10 MB.");
        }
    }

    private static byte[] readHeader(MultipartFile file) throws IOException {
        try (InputStream stream = file.getInputStream()) {
            return stream.readNBytes(16);
        }
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private static boolean startsWithAscii(byte[] bytes, String signature) {
        return asciiAt(bytes, 0, signature);
    }

    private static boolean asciiAt(byte[] bytes, int offset, String signature) {
        if (bytes.length < offset + signature.length()) return false;
        for (int i = 0; i < signature.length(); i++) {
            if (bytes[offset + i] != (byte) signature.charAt(i)) return false;
        }
        return true;
    }
}
