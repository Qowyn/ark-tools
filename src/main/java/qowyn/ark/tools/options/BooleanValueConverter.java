package qowyn.ark.tools.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

public class BooleanValueConverter implements ValueConverter<Boolean> {

  @Override
  public Boolean convert(String value) {
    if ("yes".equals(value)) {
      return Boolean.TRUE;
    } else if ("no".equals(value)) {
      return Boolean.FALSE;
    }
    
    throw new ValueConversionException("Expected yes or no but got " + value);
  }

  @Override
  public Class<? extends Boolean> valueType() {
    return Boolean.class;
  }

  @Override
  public String valuePattern() {
    return "yes|no";
  }

}
