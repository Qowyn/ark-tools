package qowyn.ark.tools.driver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qowyn.ark.tools.data.ArkDataCollector;

public class JsonDriver implements DBDriver {

  private static final List<String> PROTOCOL_LIST;

  private static final Map<String, String> PARAMETER_MAP;

  private static void testAndAddProtocol(String protocol, String testURL, List<String> protocolList) {
    try {
      new URL(testURL);
      protocolList.add(protocol);
    } catch (MalformedURLException e) {
    }
  }

  static {
    List<String> protocols = new ArrayList<>();

    testAndAddProtocol("http", "http://localhost", protocols);
    testAndAddProtocol("https", "https://localhost", protocols);
    testAndAddProtocol("file", "file://", protocols);
    testAndAddProtocol("mailto", "mailto:root@localhost", protocols);
    testAndAddProtocol("ftp", "ftp://localhost", protocols);

    PROTOCOL_LIST = Collections.unmodifiableList(protocols);

    Map<String, String> parameters = new LinkedHashMap<>();

    parameters.put("username", "username to use for http, https and ftp");
    parameters.put("password", "password to use for http, https and ftp");

    PARAMETER_MAP = Collections.unmodifiableMap(parameters);
  }

  @Override
  public void openConnection(URI uri) {
    try {
      URLConnection conn = uri.toURL().openConnection();

      conn.setDoOutput(true);
      conn.getOutputStream();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void openConnection(Path path) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public List<String> getUrlSchemeList() {
    return PROTOCOL_LIST;
  }

  @Override
  public boolean canHandlePath() {
    return true;
  }

  @Override
  public String getParameter(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DBDriver setParameter(String name, String value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, String> getSupportedParameters() {
    return PARAMETER_MAP;
  }

  @Override
  public void write(ArkDataCollector data) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

}
