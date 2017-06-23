package qowyn.ark.tools.driver;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import qowyn.ark.tools.data.DataCollector;

public interface DBDriver {

  public void openConnection(URI uri);

  public void openConnection(Path path);

  public List<String> getUrlSchemeList();

  public boolean canHandlePath();

  public String getParameter(String name);

  public DBDriver setParameter(String name, String value);

  public Map<String, String> getSupportedParameters();

  public void write(DataCollector data);

  public void close();

}
