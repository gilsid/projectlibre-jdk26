/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 */

package net.sf.mpxj.common;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that refuses to write more than a configured number of bytes.
 */
public final class LimitedOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final long limit;
    private long count;

    public LimitedOutputStream(OutputStream delegate, long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be negative");
        }
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public void write(int value) throws IOException {
        check(1);
        delegate.write(value);
        count++;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        if (offset < 0 || length < 0 || offset > bytes.length - length) {
            throw new IndexOutOfBoundsException();
        }
        check(length);
        delegate.write(bytes, offset, length);
        count += length;
    }

    private void check(int length) throws IOException {
        if (count > limit - length) {
            throw new IOException("Output exceeds the allowed size");
        }
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
