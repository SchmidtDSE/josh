package org.teavm.classlib.java.util.concurrent;

import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.ExecutorService;


public class TExecutors {

  public static ExecutorService newSingleThreadExecutor() {
    throw new NotImplementedException("Not available on WASM");
  }

}
