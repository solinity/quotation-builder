package ui;

import java.awt.Component;
import java.awt.FileDialog;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JOptionPane;

final class FileDialogs {
    private FileDialogs() {
    }

    static Path choose(Component owner, String title, int mode, Path directory, String filename) {
        FileDialog dialog = new FileDialog(JOptionPane.getFrameForComponent(owner), title, mode);
        try {
            if (directory != null) {
                dialog.setDirectory(directory.toString());
            }
            if (filename != null) {
                dialog.setFile(filename);
            }
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            if (files.length == 0) {
                return null;
            }
            return files[0].toPath().toAbsolutePath().normalize();
        } finally {
            dialog.dispose();
        }
    }

    static Path addExtension(Component owner, Path file, String extension, String title) {
        String filename = file.getFileName().toString();
        if (filename.toLowerCase(Locale.ROOT).endsWith(extension)) {
            return file;
        }

        // The file dialog only checked the name the user entered.
        Path target = file.resolveSibling(filename + extension);
        if (Files.exists(target) && JOptionPane.showConfirmDialog(owner,
                "Replace the existing file?\n" + target, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
        }
        return target;
    }
}
