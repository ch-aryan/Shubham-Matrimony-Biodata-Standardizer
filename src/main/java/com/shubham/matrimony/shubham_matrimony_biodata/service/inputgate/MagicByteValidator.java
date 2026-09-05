package com.shubham.matrimony.shubham_matrimony_biodata.service.inputgate;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Validates file magic bytes and detects spoofed extensions or unsupported binary formats.
 */
@Component
public class MagicByteValidator {

    public enum DocumentType {
        PDF("application/pdf"),
        JPEG("image/jpeg"),
        PNG("image/png"),
        WEBP("image/webp"),
        PLAIN_TEXT("text/plain");

        private final String mimeType;

        DocumentType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp", "txt");
    private static final Set<String> PROHIBITED_EXTENSIONS = Set.of(
            "docx", "doc", "zip", "rar", "7z", "tar", "gz", "exe", "dll", "bat", "sh", "bin", "iso"
    );

    // Magic byte definitions
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF
    private static final byte[] WEBP_MAGIC = new byte[]{0x57, 0x45, 0x42, 0x50}; // WEBP

    // Prohibited binary signatures
    private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK.. (ZIP, DOCX, XLSX)
    private static final byte[] EXE_MAGIC = new byte[]{0x4D, 0x5A}; // MZ (DOS/PE executable)
    private static final byte[] ELF_MAGIC = new byte[]{0x7F, 0x45, 0x4C, 0x46}; // ELF executable
    private static final byte[] RAR_MAGIC = new byte[]{0x52, 0x61, 0x72, 0x21}; // Rar!

    /**
     * Inspects filename and raw bytes to determine DocumentType or reject invalid/spoofed inputs.
     *
     * @param originalFilename client provided filename
     * @param bytes raw file payload
     * @return validated DocumentType
     */
    public DocumentType validate(String originalFilename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.LOW_INFORMATION_INPUT,
                    "Uploaded file is empty (0 bytes).");
        }

        String extension = extractExtension(originalFilename);

        // Explicit check against prohibited formats like docx, zip, exe
        if (PROHIBITED_EXTENSIONS.contains(extension)) {
            throw new InputGateException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, WarningCategory.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file format '." + extension + "'. Only PDF, JPEG, PNG, WEBP, and TXT are supported.");
        }

        // Check for dangerous binary signatures
        if (startsWith(bytes, EXE_MAGIC)) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                    "Rejected executable file disguised with extension '." + extension + "'.");
        }
        if (startsWith(bytes, ZIP_MAGIC)) {
            throw new InputGateException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, WarningCategory.UNSUPPORTED_MEDIA_TYPE,
                    "ZIP and Microsoft Office formats (.docx/.xlsx) are not supported. Please provide a PDF, image, or text.");
        }
        if (startsWith(bytes, ELF_MAGIC) || startsWith(bytes, RAR_MAGIC)) {
            throw new InputGateException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, WarningCategory.UNSUPPORTED_MEDIA_TYPE,
                    "Binary/archive file format is not supported.");
        }

        // Determine actual document type from magic bytes
        if (isPdf(bytes)) {
            if (!extension.isEmpty() && !extension.equals("pdf")) {
                throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                        "File content is PDF but file extension is '." + extension + "'.");
            }
            return DocumentType.PDF;
        }

        if (isJpeg(bytes)) {
            if (!extension.isEmpty() && !extension.equals("jpg") && !extension.equals("jpeg")) {
                throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                        "File content is JPEG but file extension is '." + extension + "'.");
            }
            return DocumentType.JPEG;
        }

        if (isPng(bytes)) {
            if (!extension.isEmpty() && !extension.equals("png")) {
                throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                        "File content is PNG but file extension is '." + extension + "'.");
            }
            return DocumentType.PNG;
        }

        if (isWebp(bytes)) {
            if (!extension.isEmpty() && !extension.equals("webp")) {
                throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                        "File content is WEBP but file extension is '." + extension + "'.");
            }
            return DocumentType.WEBP;
        }

        // If client specified extension was pdf or image, but bytes didn't match:
        if (extension.equals("pdf")) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                    "File claims to be a PDF, but header signature does not match '%PDF'.");
        }
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                    "File claims to be a JPEG, but header signature is invalid.");
        }
        if (extension.equals("png")) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                    "File claims to be a PNG, but header signature is invalid.");
        }
        if (extension.equals("webp")) {
            throw new InputGateException(HttpStatus.BAD_REQUEST, WarningCategory.INVALID_FILE_SIGNATURE,
                    "File claims to be a WEBP, but header signature is invalid.");
        }

        // Check if it's valid plain text
        if (isPlainText(bytes)) {
            return DocumentType.PLAIN_TEXT;
        }

        // If none matched, reject
        throw new InputGateException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, WarningCategory.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type or corrupted file format. Expected PDF, JPEG, PNG, WEBP, or plain text.");
    }

    private boolean isPdf(byte[] bytes) {
        return startsWith(bytes, PDF_MAGIC);
    }

    private boolean isJpeg(byte[] bytes) {
        return startsWith(bytes, JPEG_MAGIC);
    }

    private boolean isPng(byte[] bytes) {
        return startsWith(bytes, PNG_MAGIC);
    }

    private boolean isWebp(byte[] bytes) {
        if (bytes.length < 12) return false;
        return startsWith(bytes, RIFF_MAGIC) &&
                bytes[8] == WEBP_MAGIC[0] &&
                bytes[9] == WEBP_MAGIC[1] &&
                bytes[10] == WEBP_MAGIC[2] &&
                bytes[11] == WEBP_MAGIC[3];
    }

    private boolean isPlainText(byte[] bytes) {
        // Sample up to first 2048 bytes; check for null bytes or excessive control chars
        int checkLen = Math.min(bytes.length, 2048);
        int controlCharCount = 0;
        for (int i = 0; i < checkLen; i++) {
            byte b = bytes[i];
            if (b == 0x00) {
                return false; // Null byte indicates binary content
            }
            if ((b >= 0x00 && b < 0x09) || (b > 0x0D && b < 0x20)) {
                controlCharCount++;
            }
        }
        return ((double) controlCharCount / checkLen) < 0.05;
    }

    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) return false;
        }
        return true;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return filename.substring(idx + 1).trim().toLowerCase(Locale.ROOT);
    }
}
