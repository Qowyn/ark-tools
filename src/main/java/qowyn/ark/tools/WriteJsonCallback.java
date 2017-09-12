package qowyn.ark.tools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

@FunctionalInterface
public interface WriteJsonCallback {
  
  public void accept(JsonGenerator generator) throws IOException;

}
