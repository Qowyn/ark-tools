package qowyn.ark.tools;

import java.util.function.Consumer;

public class Command {

  private final String names;

  private final String category;

  private final String description;

  private final String optionSummary;

  private final String[] options;

  private final Consumer<OptionHandler> action;

  public Command(String names, String category, String description, String optionSummary, String[] options, Consumer<OptionHandler> action) {
    this.names = names;
    this.category = category;
    this.description = description;
    this.optionSummary = optionSummary;
    this.options = options;
    this.action = action;
  }

  public String getNames() {
    return names;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public String getOptionSummary() {
    return optionSummary;
  }

  public String[] getOptions() {
    return options;
  }

  public Consumer<OptionHandler> getAction() {
    return action;
  }

}
