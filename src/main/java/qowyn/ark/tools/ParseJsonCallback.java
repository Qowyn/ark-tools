package qowyn.ark.tools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;

@FunctionalInterface
public interface ParseJsonCallback {

  public void accept(JsonParser parser) throws IOException;

}
