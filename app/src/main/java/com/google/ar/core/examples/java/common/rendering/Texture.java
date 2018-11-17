package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Texture {
  private static final int SIZE = 128;

  private int width, height;
  private int textureId = -1;

  public Texture() {
    this(SIZE, SIZE);
  }

  public Texture(int width, int height) {
    assert(width > 0);
    assert(height > 0);
    this.width = width;
    this.height = height;
  }

  public int getTextureId() {
    assert(this.textureId != -1);
    return this.textureId;
  }

  public void createOnGlThread(Context context) throws IOException {
    assert(this.textureId == -1);

    // Generate the texture id
    int[] textures = new int[1];
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glGenTextures(1, textures, 0);
    this.textureId = textures[0];

    // Setup the texture
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.textureId);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
  }

  public void load(Bitmap bitmap) {
//    byte[] pixels = new byte[this.width * this.height * 4];
//    texture.getPixels(pixels, 0, 0, 0, 0, this.width, this.height);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.textureId);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, this.width, this.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
  }
}
