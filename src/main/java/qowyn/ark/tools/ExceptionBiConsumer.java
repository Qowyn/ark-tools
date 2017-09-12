package qowyn.ark.tools;

import java.io.IOException;

@FunctionalInterface
public interface ExceptionBiConsumer<T, U> {

  public void accept(T first, U second) throws IOException;

}
