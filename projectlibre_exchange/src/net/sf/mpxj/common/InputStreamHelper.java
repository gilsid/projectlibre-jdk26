/*
 * file:       InputStreamHelper.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2016
 * date:       06/06/2016
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper methods for dealing with InputStreams.
 */
public class InputStreamHelper
{
   private static final int BUFFER_SIZE = 8192;
   private static final long MAX_TEMP_FILE_BYTES = 512L * 1024 * 1024;
   private static final int MAX_ZIP_ENTRIES = 10000;
   private static final long MAX_ZIP_BYTES = 512L * 1024 * 1024;
   private static final long MAX_ZIP_ENTRY_BYTES = 256L * 1024 * 1024;
   private static final long MAX_COMPRESSION_RATIO = 10000L;

   /**
    * Copy the data from an InputStream to a temp file.
    *
    * @param inputStream data source
    * @param tempFileSuffix suffix to use for temp file
    * @return File instance
    */
   public static File writeStreamToTempFile(InputStream inputStream, String tempFileSuffix) throws IOException
   {
      File file = File.createTempFile("mpxj", tempFileSuffix);
      boolean completed = false;
      try (FileOutputStream outputStream = new FileOutputStream(file))
      {
         byte[] buffer = new byte[BUFFER_SIZE];
         long totalBytes = 0;
         int bytesRead;
         while ((bytesRead = inputStream.read(buffer)) != -1)
         {
            if (bytesRead == 0)
            {
               continue;
            }
            totalBytes += bytesRead;
            if (totalBytes > MAX_TEMP_FILE_BYTES)
            {
               throw new IOException("Temporary file exceeds the allowed size");
            }
            outputStream.write(buffer, 0, bytesRead);
         }
         completed = true;
         return file;
      }
      finally
      {
         if (!completed)
         {
            FileHelper.deleteQuietly(file);
         }
      }
   }

   /**
    * Expands a zip file input stream into a temporary directory.
    *
    * @param inputStream zip file input stream
    * @return File instance representing the temporary directory
    */
   public static File writeZipStreamToTempDir(InputStream inputStream) throws IOException
   {
      File dir = FileHelper.createTempDir();
      try (ZipInputStream zip = new ZipInputStream(inputStream))
      {
         long totalBytes = 0;
         int entryCount = 0;
         java.util.Set<String> seenEntries = new java.util.HashSet<String>();

         while (true)
         {
            ZipEntry entry = zip.getNextEntry();
            if (entry == null)
            {
               break;
            }
            if (++entryCount > MAX_ZIP_ENTRIES)
            {
               throw new IOException("Archive contains too many entries");
            }

            String entryName = entry.getName();
            if (!seenEntries.add(entryName))
            {
               throw new IOException("Archive contains a duplicate entry: " + entryName);
            }
            File file = FileHelper.resolveContainedFile(dir, entryName);
            if (entry.isDirectory())
            {
               if (!file.isDirectory() && !file.mkdirs())
               {
                  throw new IOException("Failed to create archive directory: " + entryName);
               }
               continue;
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            {
               throw new IOException("Failed to create archive directory: " + entryName);
            }

            long entryBytes = 0;
            try (OutputStream outputStream = Files.newOutputStream(file.toPath()))
            {
               byte[] bytes = new byte[BUFFER_SIZE];
               int length;
               while ((length = zip.read(bytes)) >= 0)
               {
                  if (length == 0)
                  {
                     continue;
                  }
                  entryBytes += length;
                  totalBytes += length;
                  if (entryBytes > MAX_ZIP_ENTRY_BYTES || totalBytes > MAX_ZIP_BYTES)
                  {
                     throw new IOException("Archive exceeds the allowed extracted size");
                  }
                  outputStream.write(bytes, 0, length);
               }
            }

            long compressedSize = entry.getCompressedSize();
            if (compressedSize > 0 && entryBytes > compressedSize * MAX_COMPRESSION_RATIO)
            {
               throw new IOException("Archive entry exceeds the allowed compression ratio: " + entryName);
            }
         }
         return dir;
      }
      catch (IOException | RuntimeException e)
      {
         FileHelper.deleteQuietly(dir);
         throw e;
      }
   }
}
