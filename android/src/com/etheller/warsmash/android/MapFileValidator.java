package com.etheller.warsmash.android;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

/**
 * Lightweight validation for user-supplied Warcraft III map containers.
 *
 * This checks the MPQ container signature only. It does not decrypt, extract, or
 * certify that a map version is supported by the Warsmash runtime.
 */
final class MapFileValidator {
    private static final byte[] MPQ = new byte[] { 'M', 'P', 'Q', 0x1A };
    private static final byte[] MPQ_USER_DATA = new byte[] { 'M', 'P', 'Q', 0x1B };
    private static final long USER_DATA_HEADER_OFFSET = 512L;
    private static final long MINIMUM_SIZE = 32L;

    private MapFileValidator() {
    }

    static Result inspect(final File file) {
        if (file == null) {
            return Result.invalid("No map file was selected.");
        }
        final String name = file.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".w3x") && !name.endsWith(".w3m")) {
            return Result.invalid("Choose a .w3x or .w3m Warcraft III map.");
        }
        if (!file.isFile() || !file.canRead()) {
            return Result.invalid("The selected map is not a readable file.");
        }
        if (file.length() < MINIMUM_SIZE) {
            return Result.invalid("The map file is too small to be a valid MPQ archive.");
        }
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            if (hasSignature(input, 0L, MPQ)) {
                return Result.valid(file.length(), 0L);
            }
            if ((file.length() > USER_DATA_HEADER_OFFSET + MINIMUM_SIZE)
                    && hasSignature(input, USER_DATA_HEADER_OFFSET, MPQ)) {
                return Result.valid(file.length(), USER_DATA_HEADER_OFFSET);
            }
            if (hasSignature(input, 0L, MPQ_USER_DATA)) {
                return Result.valid(file.length(), 0L);
            }
            return Result.invalid("No MPQ archive signature was found at the supported header offsets.");
        }
        catch (final IOException | SecurityException e) {
            return Result.invalid("The map file could not be inspected: " + e.getMessage());
        }
    }

    private static boolean hasSignature(final RandomAccessFile input, final long offset, final byte[] signature)
            throws IOException {
        if (offset < 0 || input.length() < offset + signature.length) {
            return false;
        }
        input.seek(offset);
        for (final byte expected : signature) {
            if (input.read() != (expected & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    static final class Result {
        final boolean validContainer;
        final long sizeBytes;
        final long archiveOffset;
        final String message;

        private Result(final boolean validContainer, final long sizeBytes, final long archiveOffset,
                final String message) {
            this.validContainer = validContainer;
            this.sizeBytes = sizeBytes;
            this.archiveOffset = archiveOffset;
            this.message = message;
        }

        static Result valid(final long sizeBytes, final long archiveOffset) {
            return new Result(true, sizeBytes, archiveOffset, "MPQ container signature found.");
        }

        static Result invalid(final String message) {
            return new Result(false, 0L, -1L, message);
        }
    }
}
