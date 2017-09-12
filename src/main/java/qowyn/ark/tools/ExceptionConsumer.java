package qowyn.ark.tools;

import java.io.IOException;

@FunctionalInterface
public interface ExceptionConsumer<T> {

  public void accept(T value) throws IOException;

}
