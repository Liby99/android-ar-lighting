package com.google.ar.core.examples.java.common.rendering;

import android.graphics.Bitmap;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.CharBuffer;

import glm.mat4x4.Mat4;
import glm.vec2.Vec2;
import glm.vec3.Vec3;
import glm.vec4.Vec4;

public class AREnvironment {
  private static final int SIZE = 128;
  private static final int HALF_SIZE = SIZE / 2;
  private static final int NUM_FACES = 6;

  // Right: 0
  // Left: 1
  // Up: 2
  // Bottom: 3
  // Back: 4
  // Front: 5
  Texture[] textures;
  Bitmap[] textureBitmaps;

  public AREnvironment() {
    textures = new Texture[NUM_FACES];
    textureBitmaps = new Bitmap[NUM_FACES];
    for (int i = 0; i < NUM_FACES; i++) {
      textures[i] = new Texture(SIZE, SIZE);
      textureBitmaps[i] = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
    }
  }

  public Texture getTexture(int i) {
    return this.textures[i];
  }

  public void update(Image cameraImage, float[] projMat, float[] viewMat) {

    // Initiate image related values
    int width = cameraImage.getWidth();
    int height = cameraImage.getHeight();
    int halfWidth = width / 2;
    int halfHeight = height / 2;

    Image.Plane[] planes = cameraImage.getPlanes();
    CharBuffer yPlane = planes[0].getBuffer().asCharBuffer();
    CharBuffer uPlane = planes[1].getBuffer().asCharBuffer();
    CharBuffer vPlane = planes[2].getBuffer().asCharBuffer();

    // Initiate matrices
    Mat4 view = new Mat4(viewMat);
    view.v30(0);
    view.v31(0);
    view.v32(0);
    Mat4 invProjView = (new Mat4(projMat)).times(view).inverse();

    // Set values
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {

        // Get the rgba values
        int pxId = j * height + i;
        int rgba = yuvToARGB(yPlane.get(pxId), uPlane.get(pxId), vPlane.get(pxId));

        // X and Y value in
        float imgx = (float) (i - halfWidth) / width;
        float imgy = (float) (j - halfHeight) / height;

        // First we get the bitmap to draw
        Vec3 pxDir = new Vec3(invProjView.times(new Vec4(imgx, imgy, 1, 1))).normalize();
        int bitmapIndex = this.getDirectionFace(pxDir);

        // Then we get the position to draw on that bitmap
        Vec2 faceXY = this.getXYOnFace(pxDir, bitmapIndex);
        int bitmapX = (int) (faceXY.x * HALF_SIZE + HALF_SIZE);
        int bitmapY = (int) (faceXY.y * HALF_SIZE + HALF_SIZE);

        // Update the bitmap
        textureBitmaps[bitmapIndex].setPixel(bitmapX, bitmapY, rgba);
      }
    }

    this.drawToTexture();
  }

  public void drawToTexture() {
    for (int i = 0; i < NUM_FACES; i++) {
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i].getTextureId());
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmaps[i], 0);
    }
  }

  public int getDirectionFace(Vec3 dir) {
    float x = Math.abs(dir.x), y = Math.abs(dir.y), z = Math.abs(dir.z);
    if (x > y) {
      if (x > z) {
        return dir.x > 0 ? 0 : 1;
      } else {
        return dir.z > 0 ? 4 : 5;
      }
    } else {
      if (y > z) {
        return dir.y > 0 ? 2 : 3;
      } else {
        return dir.z > 0 ? 4 : 5;
      }
    }
  }

  public Vec2 getXYOnFace(Vec3 dir, int face) {
    float scale, x, y, z;
    switch (face) {
      case 0: case 1:
        scale = Math.abs(1.0f / dir.x);
        y = dir.y * scale;
        z = dir.z * scale;
        return new Vec2(z, y);
      case 2: case 3:
        scale = Math.abs(1.0f / dir.y);
        x = dir.x * scale;
        z = dir.z * scale;
        return new Vec2(x, z);
      case 4: case 5:
        scale = Math.abs(1.0f / dir.z);
        x = dir.x * scale;
        y = dir.y * scale;
        return new Vec2(x, y);
      default:
        throw new Error("Invalid face " + face);
    }
  }

  public int yuvToARGB(char y, char u, char v) {
    float fu = (float) u, fv = (float) v;
    char r = (char) (y + 1.14f * fv);
    char g = (char) (y - 0.395f * fu - 0.581f * fv);
    char b = (char) (y + 2.033f * fu);
    return 0xff000000 | r >> 8 | g >> 16 | b >> 24;
  }
}
