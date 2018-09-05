package tech.testra.jvm.gatling.plugin.parseUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.zip.GZIPInputStream;
import net.quux00.simplecsv.CsvParser;
import net.quux00.simplecsv.CsvParserBuilder;
import net.quux00.simplecsv.CsvReader;

public class SimReader  extends CsvReader {

  protected static final String ASSERTION = "assertion";

  public SimReader(File file) throws IOException {
    this(getReaderFor(file), 0,
        new CsvParserBuilder().trimWhitespace(true).allowUnbalancedQuotes(true).separator('\t').build());
  }

  public SimReader(Reader reader, int line, CsvParser csvParser) {
    super(reader, line, csvParser);
  }

  @Override
  public List<String> readNext() throws IOException {
    List<String> ret = super.readNext();
    if (ret != null && !ret.isEmpty() && ret.get(0).toLowerCase().startsWith(ASSERTION)) {
      return readNext();
    }
    return ret;
  }

  public static Reader getReaderFor(File file) throws IOException {
    if ("gz".equals(getFileExtension(file))) {
      InputStream fileStream = new FileInputStream(file);
      InputStream gzipStream = new GZIPInputStream(fileStream);
      return new InputStreamReader(gzipStream, "UTF-8");
    }
    return new FileReader(file);
  }

  public static String getFileExtension(File file) {
    String name = file.getName();
    try {
      return name.substring(name.lastIndexOf(".") + 1);
    } catch (Exception e) {
      return "";
    }
  }

}
