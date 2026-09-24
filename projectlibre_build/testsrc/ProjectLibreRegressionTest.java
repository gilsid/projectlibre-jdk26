import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.projectlibre1.util.Environment;
import com.projectlibre1.util.SerializationFilter;
import net.sf.mpxj.common.FileHelper;
import net.sf.mpxj.common.InputStreamHelper;
import net.sf.mpxj.common.LimitedOutputStream;

public class ProjectLibreRegressionTest {
    public static void main(String[] args) throws Exception {
        testPlatformDetection();
        testFileRouting();
        testDeserializationFilter();
        testOutputLimit();
        testZipExtractionContainsEntries();
        testZipExtractionRejectsTraversal();
        System.out.println("ProjectLibre regression checks passed");
    }

    private static void testPlatformDetection() {
        String original = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");
            check(Environment.isWindows(), "Windows should be detected");
            System.setProperty("os.name", "Linux");
            check(!Environment.isWindows(), "Linux should not be detected as Windows");
        } finally {
            if (original == null) {
                System.clearProperty("os.name");
            } else {
                System.setProperty("os.name", original);
            }
        }
    }

    private static void testFileRouting() {
        check(com.projectlibre1.session.FileHelper.isFileNameAllowed("PROJECT.POD", false), "Uppercase project extension was rejected");
        check(com.projectlibre1.session.FileHelper.isFileNameAllowed("PROJECT.POD", true), "Uppercase save extension was rejected");
    }

    private static void testDeserializationFilter() throws Exception {
        List<String> values = new ArrayList<>();
        values.add("safe");
        byte[] allowed = serialize(values);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(allowed))) {
            input.setObjectInputFilter(SerializationFilter.get());
            check(values.equals(input.readObject()), "Allowed project data was rejected");
        }

        byte[] rejected = serialize(new UntrustedValue());
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(rejected))) {
            input.setObjectInputFilter(SerializationFilter.get());
            input.readObject();
            throw new AssertionError("Unexpected class passed the deserialization filter");
        } catch (InvalidClassException expected) {
            check(true, "Unexpected filter failure");
        }
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static void testOutputLimit() throws Exception {
        try (java.io.OutputStream output = new LimitedOutputStream(new ByteArrayOutputStream(), 3)) {
            output.write(new byte[4]);
            throw new AssertionError("Output limit was not enforced");
        } catch (IOException expected) {
            check(true, "Unexpected output limit failure");
        }
    }

    private static void testZipExtractionContainsEntries() throws Exception {
        byte[] archive = zip("nested/value.txt", "ok".getBytes(StandardCharsets.UTF_8));
        File directory = InputStreamHelper.writeZipStreamToTempDir(new ByteArrayInputStream(archive));
        try {
            File extracted = new File(directory, "nested/value.txt");
            check(extracted.isFile(), "ZIP entry was not extracted");
            check("ok".equals(Files.readString(extracted.toPath())), "ZIP entry content changed");
        } finally {
            FileHelper.deleteQuietly(directory);
        }
    }

    private static void testZipExtractionRejectsTraversal() throws Exception {
        byte[] archive = zip("../outside.txt", "bad".getBytes(StandardCharsets.UTF_8));
        try {
            InputStreamHelper.writeZipStreamToTempDir(new ByteArrayInputStream(archive));
            throw new AssertionError("ZIP traversal was accepted");
        } catch (IOException expected) {
            check(expected.getMessage().contains("escapes"), "Unexpected traversal error");
        }
    }

    private static byte[] zip(String name, byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            output.putNextEntry(new ZipEntry(name));
            output.write(content);
            output.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static final class UntrustedValue implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
