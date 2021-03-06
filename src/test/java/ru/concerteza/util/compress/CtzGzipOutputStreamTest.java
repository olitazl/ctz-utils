package ru.concerteza.util.compress;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Test;
import ru.concerteza.util.io.noclose.NoCloseOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * User: alexkasko
 * Date: 10/21/14
 */
public class CtzGzipOutputStreamTest {
    private static final int LENGTH = 8096;
    private static final int CHUNK_LENGTH = 320;

    @Test
    public void test() throws IOException {
        // generate data
        Random ra = new Random(42);
        byte[] source = new byte[LENGTH];
        ra.nextBytes(source);

        // compress chunks per 320 bytes
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        OutputStream noclose = new NoCloseOutputStream(expected);
        for (int i = 0; i < source.length / CHUNK_LENGTH; i++) {
            OutputStream gz = new GZIPOutputStream(noclose);
            gz.write(source, i * CHUNK_LENGTH, CHUNK_LENGTH);
            gz.close();
        }

        // compress the same without objects recreation
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        CtzGzipOutputStream resettable = new CtzGzipOutputStream(actual);
        for (int i = 0; i < source.length / CHUNK_LENGTH; i++) {
            resettable.write(source, i * CHUNK_LENGTH, CHUNK_LENGTH);
            if(i + 1 < source.length / CHUNK_LENGTH) { // do not reset immediately before close
                resettable.reset();
            }
        }
        resettable.close();

        Assert.assertArrayEquals(expected.toByteArray(), actual.toByteArray());
    }
}
