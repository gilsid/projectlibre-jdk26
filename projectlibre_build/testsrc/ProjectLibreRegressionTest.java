import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
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
import net.sf.mpxj.common.LimitedInputStream;
import net.sf.mpxj.common.LimitedOutputStream;

public class ProjectLibreRegressionTest {
    public static void main(String[] args) throws Exception {
        testPlatformDetection();
        testFileRouting();
        testDeserializationFilter();
        testDeserializationFilterRejectsUrl();
        testDeserializationFilterAllowsUriAndPrintTypes();
        testDeserializationFilterAllowsPageFormat();
        testDeserializationFilterAllowsMoney();
        testDeserializationFilterRejectsHugeArray();
        testOutputLimit();
        testInputLimit();
        testTempDirLifecycle();
        testZipExtractionContainsEntries();
        testZipExtractionRejectsTraversal();
        testZipExtractionRejectsAbsoluteEntry();
        testZipExtractionRejectsDuplicateEntry();
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

    private static void testInputLimit() throws Exception {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        try (LimitedInputStream input = new LimitedInputStream(new ByteArrayInputStream(data), 3)) {
            byte[] buffer = new byte[10];
            int first = input.read(buffer, 0, 10);
            check(first == 3, "Input was not capped at the limit");
            try {
                input.read();
                throw new AssertionError("Input limit was not enforced");
            } catch (IOException expected) {
                check(true, "Unexpected input limit failure");
            }
        }
    }

    private static void testTempDirLifecycle() throws Exception {
        File directory = FileHelper.createTempDir();
        try {
            check(directory.isDirectory(), "Temporary directory was not created");
        } finally {
            FileHelper.deleteQuietly(directory);
        }
        check(!directory.exists(), "Temporary directory was not removed");
    }

    private static void testDeserializationFilterRejectsUrl() throws Exception {
        byte[] rejected = serialize(java.net.URI.create("https://example.com/").toURL());
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(rejected))) {
            input.setObjectInputFilter(SerializationFilter.get());
            input.readObject();
            throw new AssertionError("URL passed the deserialization filter");
        } catch (InvalidClassException expected) {
            check(true, "Unexpected filter failure");
        }
    }

    private static void testDeserializationFilterAllowsUriAndPrintTypes() throws Exception {
        List<Object> values = new ArrayList<>();
        values.add(java.net.URI.create("https://example.com/"));
        values.add(java.math.BigDecimal.ONE);
        values.add(java.time.LocalDate.of(2026, 9, 25));
        byte[] allowed = serialize((Serializable) values);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(allowed))) {
            input.setObjectInputFilter(SerializationFilter.get());
            Object result = input.readObject();
            check(values.equals(result), "Allowed URI/value data was rejected");
        }
    }

    private static void testDeserializationFilterAllowsPageFormat() throws Exception {
        com.projectlibre1.print.ExtendedPageFormat format = new com.projectlibre1.print.ExtendedPageFormat();
        byte[] allowed = serialize(format);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(allowed))) {
            input.setObjectInputFilter(SerializationFilter.get());
            Object result = input.readObject();
            check(result instanceof com.projectlibre1.print.ExtendedPageFormat,
                    "Saved page format was rejected by the deserialization filter");
        }
    }

    private static void testDeserializationFilterAllowsMoney() throws Exception {
        com.projectlibre1.datatype.Money money = com.projectlibre1.datatype.Money.getInstance(123.45);
        byte[] allowed = serialize(money);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(allowed))) {
            input.setObjectInputFilter(SerializationFilter.get());
            Object result = input.readObject();
            check(money.equals(result), "Saved money value was rejected by the deserialization filter");
        }
    }

    private static void testDeserializationFilterRejectsHugeArray() throws Exception {
        check(filterStatus(null, 500_000_000L) == ObjectInputFilter.Status.REJECTED,
                "Oversized array was not rejected by the deserialization filter");
        check(filterStatus(null, 100L) != ObjectInputFilter.Status.REJECTED,
                "Small array was rejected by the deserialization filter");
        check(filterStatus(null, -1L) != ObjectInputFilter.Status.REJECTED,
                "Null class without array length was rejected by the deserialization filter");
        check(filterStatus(java.net.URL.class, -1L) == ObjectInputFilter.Status.REJECTED,
                "URL class was not rejected by the deserialization filter");
        check(filterStatus(java.util.ArrayList.class, -1L) == ObjectInputFilter.Status.ALLOWED,
                "Allowed collection class was rejected by the deserialization filter");
    }

    private static ObjectInputFilter.Status filterStatus(Class<?> serialClass, long arrayLength) {
        return SerializationFilter.get().checkInput(new FilterProbe(serialClass, arrayLength));
    }

    private static final class FilterProbe implements ObjectInputFilter.FilterInfo {
        private final Class<?> serialClass;
        private final long arrayLength;

        FilterProbe(Class<?> serialClass, long arrayLength) {
            this.serialClass = serialClass;
            this.arrayLength = arrayLength;
        }

        @Override
        public Class<?> serialClass() {
            return serialClass;
        }

        @Override
        public long arrayLength() {
            return arrayLength;
        }

        @Override
        public long depth() {
            return 1;
        }

        @Override
        public long references() {
            return 1;
        }

        @Override
        public long streamBytes() {
            return 0;
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

    private static void testZipExtractionRejectsAbsoluteEntry() throws Exception {
        byte[] archive = zip("/absolute.txt", "bad".getBytes(StandardCharsets.UTF_8));
        try {
            InputStreamHelper.writeZipStreamToTempDir(new ByteArrayInputStream(archive));
            throw new AssertionError("Absolute ZIP entry was accepted");
        } catch (IOException expected) {
            check(true, "Unexpected absolute entry failure");
        }
    }

    private static void testZipExtractionRejectsDuplicateEntry() throws Exception {
        byte[] archive = zipMany(
                new String[] {"dup.txt", "dup.txt"},
                new byte[][] {"first".getBytes(StandardCharsets.UTF_8), "second".getBytes(StandardCharsets.UTF_8)});
        try {
            InputStreamHelper.writeZipStreamToTempDir(new ByteArrayInputStream(archive));
            throw new AssertionError("Duplicate ZIP entry was accepted");
        } catch (IOException expected) {
            check(expected.getMessage().contains("duplicate"), "Unexpected duplicate entry error");
        }
    }

    private static byte[] zipMany(String[] names, byte[][] contents) throws IOException {
        // Built by hand: java.util.zip refuses to write duplicate entries,
        // but hostile archives contain them, and the extractor must refuse.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        int[] offsets = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            offsets[i] = bytes.size();
            byte[] name = names[i].getBytes(StandardCharsets.UTF_8);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(contents[i]);
            writeLeInt(out, 0x04034b50);
            writeLeShort(out, 20);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeInt(out, (int) crc.getValue());
            writeLeInt(out, contents[i].length);
            writeLeInt(out, contents[i].length);
            writeLeShort(out, name.length);
            writeLeShort(out, 0);
            out.write(name);
            out.write(contents[i]);
        }
        int centralStart = bytes.size();
        for (int i = 0; i < names.length; i++) {
            byte[] name = names[i].getBytes(StandardCharsets.UTF_8);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(contents[i]);
            writeLeInt(out, 0x02014b50);
            writeLeShort(out, 20);
            writeLeShort(out, 20);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeInt(out, (int) crc.getValue());
            writeLeInt(out, contents[i].length);
            writeLeInt(out, contents[i].length);
            writeLeShort(out, name.length);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeShort(out, 0);
            writeLeInt(out, 0);
            writeLeInt(out, offsets[i]);
            out.write(name);
        }
        int centralSize = bytes.size() - centralStart;
        writeLeInt(out, 0x06054b50);
        writeLeShort(out, 0);
        writeLeShort(out, 0);
        writeLeShort(out, names.length);
        writeLeShort(out, names.length);
        writeLeInt(out, centralSize);
        writeLeInt(out, centralStart);
        writeLeShort(out, 0);
        out.flush();
        return bytes.toByteArray();
    }

    private static void writeLeShort(java.io.DataOutput out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
    }

    private static void writeLeInt(java.io.DataOutput out, int value) throws IOException {
        writeLeShort(out, value & 0xFFFF);
        writeLeShort(out, (value >>> 16) & 0xFFFF);
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
