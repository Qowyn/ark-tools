package qowyn.ark.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionDescriptor;

public class FilteredHelpFormatter extends BuiltinHelpFormatter {

  private final Set<String> ignore;

  public FilteredHelpFormatter(Set<String> ignore) {
    super(80, 2);
    this.ignore = new HashSet<>(ignore);
  }

  @Override
  public String format(Map<String, ? extends OptionDescriptor> options) {
    Map<String, OptionDescriptor> filteredOptions = new HashMap<>();

    options.forEach((key, option) -> {
      if (!ignore.contains(key) || option.representsNonOptions()) {
        filteredOptions.put(key, option);
      }
    });

    return super.format(filteredOptions);
  }

}
