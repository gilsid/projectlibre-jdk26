package net.sf.mpxj.common;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that refuses to deliver more than a configured number of bytes.
 * Used to bound decompression of embedded archives before the bytes reach a
 * deserializer.
 */
public final class LimitedInputStream extends FilterInputStream
{
   private final long limit;
   private long count;

   /**
    * @param input wrapped stream
    * @param limit maximum number of bytes that may be read
    */
   public LimitedInputStream(InputStream input, long limit)
   {
      super(input);
      if (input == null)
      {
         throw new NullPointerException("input");
      }
      if (limit < 0)
      {
         throw new IllegalArgumentException("Limit must not be negative");
      }
      this.limit = limit;
   }

   @Override public int read() throws IOException
   {
      if (count >= limit)
      {
         throw new IOException("Input exceeds the allowed size");
      }
      int result = super.read();
      if (result >= 0)
      {
         count++;
      }
      return result;
   }

   @Override public int read(byte[] bytes, int offset, int length) throws IOException
   {
      if (length == 0)
      {
         return 0;
      }
      if (count >= limit)
      {
         throw new IOException("Input exceeds the allowed size");
      }
      int allowed = (int) Math.min(length, limit - count);
      int result = super.read(bytes, offset, allowed);
      if (result > 0)
      {
         count += result;
      }
      return result;
   }
}
