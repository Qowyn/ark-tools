package qowyn.ark.tools.driver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import qowyn.ark.tools.data.DataCollector;

public interface DBDriver {

  public void openConnection(URI uri) throws IOException;

  public void openConnection(Path path) throws IOException;

  public List<String> getUrlSchemeList();

  public boolean canHandlePath();

  public String getParameter(String name);

  public DBDriver setParameter(String name, String value);

  public Map<String, String> getSupportedParameters();

  public void write(DataCollector data) throws IOException;

  public void close();

}
