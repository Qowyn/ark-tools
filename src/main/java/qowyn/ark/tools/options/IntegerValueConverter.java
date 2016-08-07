package qowyn.ark.tools.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

public class IntegerValueConverter implements ValueConverter<Integer> {

  @Override
  public Integer convert(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException nfe) {
      throw new ValueConversionException(value + " is not a valid number", nfe);
    }
  }

  @Override
  public Class<? extends Integer> valueType() {
    return Integer.class;
  }

  @Override
  public String valuePattern() {
    return "number";
  }

}
