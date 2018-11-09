package com.google.ar.core.examples.java.common.rendering;

public class BackgroundRenderer extends QuadRenderer {

  private static final float[] QUAD_COORDS =
          {-1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f};

  public BackgroundRenderer() {
    super(QUAD_COORDS);
  }
}
