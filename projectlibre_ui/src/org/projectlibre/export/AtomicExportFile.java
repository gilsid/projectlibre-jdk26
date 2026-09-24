package org.projectlibre.export;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

final class AtomicExportFile {
    private AtomicExportFile() {
    }

    static File createSiblingTempFile(File target) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        String prefix=target.getName();
        if (prefix.length() < 3) {
            prefix="pl"+prefix;
        }
        return File.createTempFile(prefix, ".tmp", parent);
    }

    static void replace(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
