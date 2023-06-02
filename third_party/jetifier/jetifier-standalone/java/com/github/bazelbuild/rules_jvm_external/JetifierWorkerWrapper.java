package com.github.bazelbuild.rules_jvm_external;

import com.android.tools.build.jetifier.standalone.Main;
import com.google.devtools.build.lib.worker.ProtoWorkerMessageProcessor;
import com.google.devtools.build.lib.worker.WorkRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;

/**
 * A Wrapper for jetifier-standalone to support multiplex worker
 */
public class JetifierWorkerWrapper {

  public static void main(String[] args) {
    if (args.length == 1 && args[0].equals("--persistent_worker")) {
      WorkRequestHandler workerHandler =
          new WorkRequestHandler.WorkRequestHandlerBuilder(
              JetifierWorkerWrapper::jetifier,
              System.err,
              new ProtoWorkerMessageProcessor(System.in, System.out))
              .setCpuUsageBeforeGc(Duration.ofSeconds(10))
              .build();
      int exitCode = 1;
      try {
        workerHandler.processRequests();
        exitCode = 0;
      } catch (IOException e) {
        System.err.println(e.getMessage());
      } finally {
        // Prevent hanging threads from keeping the worker alive.
        System.exit(exitCode);
      }
    } else {
      Main.main(args);
    }
  }

  private static int jetifier(List<String> args, PrintWriter pw) {
    Main.main(args.toArray(new String[0]));
    return 0;
  }

}