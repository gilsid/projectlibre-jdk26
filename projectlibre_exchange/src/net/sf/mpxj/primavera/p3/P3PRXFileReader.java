/*
 * file:       P3PRXFileReader.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2018
 * date:       11/03/2018
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

package net.sf.mpxj.primavera.p3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.common.FileHelper;
import net.sf.mpxj.common.FixedLengthInputStream;
import net.sf.mpxj.common.LimitedOutputStream;
import net.sf.mpxj.common.StreamHelper;
import net.sf.mpxj.listener.ProjectListener;
import net.sf.mpxj.primavera.common.Blast;
import net.sf.mpxj.reader.AbstractProjectReader;

/**
 * Reads a schedule data from a P3 PRX file.
 */
public final class P3PRXFileReader extends AbstractProjectReader
{
   private static final long MAX_EXTRACTED_FILE_BYTES = 256L * 1024 * 1024;
   private static final int MAX_ARCHIVE_ENTRIES = 10000;
   private static final long MAX_ARCHIVE_BYTES = 512L * 1024 * 1024;
   @Override public void addProjectListener(ProjectListener listener)
   {
      if (m_projectListeners == null)
      {
         m_projectListeners = new LinkedList<ProjectListener>();
      }
      m_projectListeners.add(listener);
   }

   @Override public ProjectFile read(InputStream stream) throws MPXJException
   {
      File tempDir = null;

      try
      {
         StreamHelper.skip(stream, 27000);
         tempDir = FileHelper.createTempDir();

         int entryCount = 0;
         long totalBytes = 0;
         while (stream.available() > 0)
         {
            if (++entryCount > MAX_ARCHIVE_ENTRIES)
            {
               throw new IOException("P3 archive contains too many entries");
            }
            totalBytes += extractFile(stream, tempDir);
            if (totalBytes > MAX_ARCHIVE_BYTES)
            {
               throw new IOException("P3 archive exceeds the allowed extracted size");
            }
         }

         return P3DatabaseReader.setProjectNameAndRead(tempDir);
      }

      catch (IOException ex)
      {
         throw new MPXJException("Failed to parse file", ex);
      }

      finally
      {
         FileHelper.deleteQuietly(tempDir);
      }
   }

   /**
    * Extracts the data for a single file from the input stream and writes
    * it to a target directory.
    *
    * @param stream input stream
    * @param dir target directory
    */
   private long extractFile(InputStream stream, File dir) throws IOException
   {
      byte[] header = new byte[8];
      byte[] fileName = new byte[13];
      byte[] dataSize = new byte[4];

      header = stream.readNBytes(8);
      fileName = stream.readNBytes(13);
      dataSize = stream.readNBytes(4);
      if (header.length < 8 || fileName.length < 13 || dataSize.length < 4)
      {
         throw new IOException("Truncated P3 archive entry");
      }


      int dataSizeValue = getInt(dataSize, 0);
      if (dataSizeValue < 0 || dataSizeValue > MAX_EXTRACTED_FILE_BYTES)
      {
         throw new IOException("P3 archive entry is too large");
      }
      String fileNameValue = getString(fileName, 0);
      File file = FileHelper.resolveContainedFile(dir, fileNameValue);
      File parent = file.getParentFile();
      if (parent != null && !parent.isDirectory() && !parent.mkdirs())
      {
         throw new IOException("Failed to create P3 archive directory");
      }

      if (dataSizeValue == 0)
      {
         FileHelper.createNewFile(file);
         return 0;
      }
      else
      {
         try (FixedLengthInputStream inputStream = new FixedLengthInputStream(stream, dataSizeValue);
              OutputStream os = new LimitedOutputStream(new FileOutputStream(file), MAX_EXTRACTED_FILE_BYTES))
         {
            Blast blast = new Blast();
            blast.blast(inputStream, os);
         }
         return file.length();
      }
   }

   /**
    * Retrieve a four byte integer.
    *
    * @param data byte array
    * @param offset offset into array
    * @return int value
    */
   private int getInt(byte[] data, int offset)
   {
      int result = 0;
      int i = offset;
      for (int shiftBy = 0; shiftBy < 32; shiftBy += 8)
      {
         result |= ((data[i] & 0xff)) << shiftBy;
         ++i;
      }
      return result;
   }

   /**
    * Retrieve a string from the byte array.
    *
    * @param data byte array
    * @param offset offset into byte array
    * @return String instance
    */
   private String getString(byte[] data, int offset)
   {
      StringBuilder buffer = new StringBuilder();
      char c;

      for (int loop = 0; offset + loop < data.length; loop++)
      {
         c = (char) data[offset + loop];

         if (c == 0)
         {
            break;
         }

         buffer.append(c);
      }

      return buffer.toString();
   }

   private List<ProjectListener> m_projectListeners;
}
