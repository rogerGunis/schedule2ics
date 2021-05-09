package de.gunis.roger.jobService.exports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


public class FileExtractorFromJar {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static URI with(String fileName, String targetDirectory) throws URISyntaxException, IOException {
    return getFile(getJarURI(), fileName, targetDirectory);
  }

  private static URI getJarURI()
      throws URISyntaxException {
    final ProtectionDomain domain;
    final CodeSource source;
    final URL url;
    final URI uri;

    domain = FileExtractorFromJar.class.getProtectionDomain();
    source = domain.getCodeSource();
    url = source.getLocation();
    uri = url.toURI();

    return (uri);
  }

  private static URI getFile(final URI where,
                             final String fileName, String targetDirectory)
      throws ZipException,
      IOException {
    final File location;
    final URI fileURI;

    location = new File(where);

    // not in a JAR, just return the path on disk
    if (location.isDirectory()) {
      fileURI = URI.create(where.toString() + fileName);
    } else {
      final ZipFile zipFile;

      zipFile = new ZipFile(location);

      try {
        fileURI = extract(zipFile, fileName, targetDirectory);
      } finally {
        zipFile.close();
      }
    }

    return (fileURI);
  }

  private static URI extract(final ZipFile zipFile,
                             final String fileName, final String targetDirectory)
      throws IOException {
    final File tempFile;
    final ZipEntry entry;
    final InputStream zipStream;
    OutputStream fileStream;

//    tempFile = File.createTempFile(fileName, Long.toString(System.currentTimeMillis()));
    tempFile = new File(targetDirectory+"/"+fileName);
    logger.info(tempFile.toString());
    tempFile.createNewFile();
//    tempFile.deleteOnExit();
    entry = zipFile.getEntry(fileName);

    if (entry == null) {
      throw new FileNotFoundException("cannot find file: " + fileName + " in archive: " + zipFile.getName());
    }

    zipStream = zipFile.getInputStream(entry);
    fileStream = null;

    try {
      final byte[] buf;
      int i;

      fileStream = new FileOutputStream(tempFile);
      buf = new byte[1024];
      i = 0;

      while ((i = zipStream.read(buf)) != -1) {
        fileStream.write(buf, 0, i);
      }
    } finally {
      close(zipStream);
      close(fileStream);
    }
    return (tempFile.toURI());
  }

  private static void close(final Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (final IOException ex) {
        ex.printStackTrace();
      }
    }
  }
}