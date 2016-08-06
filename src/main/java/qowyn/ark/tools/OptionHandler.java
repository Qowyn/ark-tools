package qowyn.ark.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

public class OptionHandler {

  private final OptionParser parser;

  private final OptionSpec<String> nonOptionsSpec;

  private final OptionSpec<Boolean> mmapSpec;

  private final OptionSpec<Boolean> parallelSpec;

  private final OptionSpec<Boolean> stopwatchSpec;

  private final OptionSpec<Void> quietSpec;

  private final OptionSpec<Void> helpSpec;

  private final String[] originalArgs;

  private final OptionSet initialOptions;

  private final List<String> nonOptions;

  public OptionHandler(String... args) {
    parser = new OptionParser();
    parser.allowsUnrecognizedOptions();
    
    nonOptionsSpec = parser.nonOptions();
    mmapSpec = parser.acceptsAll(Arrays.asList("mmap", "m"), "False if the file should be read directly to memory, true if memory mapping should be used.").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    parallelSpec = parser.acceptsAll(Arrays.asList("parallel", "p"), "True if the file should be read with multiple threads.").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    stopwatchSpec = parser.acceptsAll(Arrays.asList("stopwatch", "s"), "True if the time spent for the task should be measured.").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    helpSpec = parser.acceptsAll(Arrays.asList("help", "h"), "Displays this help screen, use with a command specified to get contextual help.").forHelp();
    quietSpec = parser.acceptsAll(Arrays.asList("quiet", "q"), "Surpresses output, except for stopwatch and help.");
    initialOptions = parser.parse(args);
    originalArgs = args;
    nonOptions = nonOptionsSpec.values(initialOptions);
  }

  public boolean hasCommand() {
    return nonOptions.size() > 0;
  }

  public String getCommand() {
    return nonOptions.get(0);
  }

  public List<String> getParams() {
    return nonOptions.subList(1, nonOptions.size());
  }

  public OptionSet reparse() {
    return parser.parse(originalArgs);
  }

  public List<String> getParams(OptionSet options) {
    return parser.nonOptions().values(options);
  }

  public boolean useMmap() {
    return mmapSpec.value(initialOptions);
  }

  public boolean useParallel() {
    return parallelSpec.value(initialOptions);
  }

  public boolean useStopwatch() {
    return stopwatchSpec.value(initialOptions);
  }

  public boolean wantsHelp() {
    return initialOptions.has(helpSpec);
  }

  public boolean isQuiet() {
    return initialOptions.has(quietSpec);
  }

  public void printHelp() {
    try {
      System.out.println();
      parser.printHelpOn(System.out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public OptionSpecBuilder accepts(String option, String description) {
    return parser.accepts(option, description);
  }

  public OptionSpecBuilder acceptsAll(List<String> options, String description) {
    return parser.acceptsAll(options, description);
  }

}
