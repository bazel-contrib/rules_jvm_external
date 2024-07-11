package com.google.ar.sceneform.assets;

/**
 * Some class in com.google.ar.sceneform:rendering:aar:1.10.0 loads
 * this class, but there is no class Loader in
 * com.google.ar.sceneform:assets:1.10.0 or any other aar in sceneform.
 * This is only to satisfy the ImportDepsChecker check since none of
 * this code is actually run.
 */
public class Loader {
  public static boolean loadUnifiedJni() { 
    return false;
  }
}
