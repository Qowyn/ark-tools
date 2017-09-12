package qowyn.ark.tools.data;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

@FunctionalInterface
public interface WriterFunction<T> {
  
  public void accept(T object, JsonGenerator generator, DataContext context, boolean writeEmpty) throws IOException;

}
