package com.google.ar.core.examples.java.common.rendering;

import android.graphics.Bitmap;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import glm.mat4x4.Mat4;
import glm.vec2.Vec2;
import glm.vec3.Vec3;
import glm.vec4.Vec4;

public class AREnvironment {
  private static final int SIZE = 64;
  private static final int HALF_SIZE = SIZE / 2;
  private static final int NUM_FACES = 6;
  private static final int SKIP = 15;

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

    Log.d("Liby", "Updating AR Env");

    // Initiate image related values
    int width = cameraImage.getWidth();
    int height = cameraImage.getHeight();
    int halfWidth = width / 2;
    int halfHeight = height / 2;

    Image.Plane[] planes = cameraImage.getPlanes();
    ByteBuffer yPlane = planes[0].getBuffer();
    ByteBuffer uPlane = planes[1].getBuffer();
    ByteBuffer vPlane = planes[2].getBuffer();

    // Initiate matrices
    Mat4 view = new Mat4(viewMat);
    view.v30(0);
    view.v31(0);
    view.v32(0);

    Mat4 invProjView = (new Mat4(projMat)).times(view).inverse();

    final int total = yPlane.capacity();
    final int uvCapacity = uPlane.capacity();

    // Set values
    int yPos = 0, uvPos = 0;
    for (int i = 0; i < height; i += SKIP) {
      for (int j = 0; j < width; j += SKIP) {
        if (uvPos >= uvCapacity - 1) {
          break;
        } else if (yPos >= total) {
          break;
        }

        // Get the rgba values
        yPos = i * width + j;
        uvPos = ((i >> 1) * width) + (j / 2 * 2);
        int y = yPlane.get(yPos) & 0xff;
        int u = (uPlane.get(uvPos) & 0xff) - 128;
        int v = (vPlane.get(uvPos + 1) & 0xff) - 128;
        int argb = yuvToARGB(y, u, v);

        // X and Y value in
        float imgx = (float) (j + halfWidth) / halfWidth;
        float imgy = (float) (-i + halfHeight) / halfHeight;

        // First we get the bitmap to draw
        Vec3 pxDir = new Vec3(invProjView.times(new Vec4(imgx, imgy, 1, 1))).normalize();
        int bitmapIndex = this.getDirectionFace(pxDir);

        // Then we get the position to draw on that bitmap
        Vec2 faceXY = this.getXYOnFace(pxDir, bitmapIndex);
        int bitmapX = (int) (faceXY.x * HALF_SIZE + HALF_SIZE);
        int bitmapY = (int) (-faceXY.y * HALF_SIZE + HALF_SIZE);

        Log.d("Liby", "Face " + bitmapIndex + ", x = " + bitmapX + ", y = " + bitmapY + ", color = " + argb);

        // Update the bitmap
        textureBitmaps[bitmapIndex].setPixel(bitmapX, bitmapY, argb);
      }
    }
  }

  public void drawToTexture() {
    for (int i = 0; i < NUM_FACES; i++) {
      textures[i].load(textureBitmaps[i]);
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

  public int sign(float n) {
    return n > 0 ? 1 : -1;
  }

  public Vec2 getXYOnFace(Vec3 dir, int face) {
    float scale, x, y, z;
    switch (face) {
      case 0: case 1:
        scale = Math.abs(1.0f / dir.x);
        y = sign(dir.x) * dir.y * scale;
        z = sign(dir.x) * dir.z * scale;
        return new Vec2(z, y);
      case 2: case 3:
        scale = Math.abs(1.0f / dir.y);
        x = -sign(dir.y) * dir.x * scale;
        z = -sign(dir.y) * dir.z * scale;
        return new Vec2(x, z);
      case 4: case 5:
        scale = Math.abs(1.0f / dir.z);
        x = -sign(dir.z) * dir.x * scale;
        y = sign(dir.z) * dir.y * scale;
        return new Vec2(x, y);
      default:
        throw new Error("Invalid face " + face);
    }
  }

  public int yuvToARGB(int y1, int u, int v) {
    final int y1192 = 1192 * y1;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
    g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
    b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }
}
