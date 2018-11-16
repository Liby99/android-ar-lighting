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
  private static final int SIZE = 16;
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

//    Log.d("Liby", new Mat4(new float[] {1, 2, 3}).toString());

    Mat4 invProjView = (new Mat4(projMat)).times(view).inverse();

//    Log.d("Liby", "Starting to update bitmaps, width = " + width + ", height = " + height);

    final int total = yPlane.capacity();
    final int uvCapacity = uPlane.capacity();

    // Set values
    int yPos = 0;
    for (int i = 0; i < height; i += 4) {

      int uvPos = (i >> 1) * width;
      for (int j = 0; j < width; j += 4) {
        if (uvPos >= uvCapacity - 1) {
          break;
        } else if (yPos >= total) {
          break;
        }

        // Get the rgba values
        int y = yPlane.get(yPos++) & 0xff;
        int u = (uPlane.get(uvPos) & 0xff) - 128;
        int v = (vPlane.get(uvPos + 1) & 0xff) - 128;
        if ((j & 1) == 1) {
          uvPos += 2;
        }
        int argb = yuvToARGB(y, u, v);

        // X and Y value in
        float imgx = (float) (i - halfHeight) / halfHeight;
        float imgy = (float) (j - halfWidth) / halfWidth;

        // First we get the bitmap to draw
        Vec3 pxDir = new Vec3(invProjView.times(new Vec4(imgx, imgy, 1, 1))).normalize();
        int bitmapIndex = this.getDirectionFace(pxDir);

        // Then we get the position to draw on that bitmap
        Vec2 faceXY = this.getXYOnFace(pxDir, bitmapIndex);
        int bitmapX = (int) (faceXY.x * HALF_SIZE + HALF_SIZE);
        int bitmapY = (int) (faceXY.y * HALF_SIZE + HALF_SIZE);

        Log.d("Liby", "Face " + bitmapIndex + ", x = " + bitmapX + ", y = " + bitmapY + ", color = " + argb);

        // Update the bitmap
        textureBitmaps[bitmapIndex].setPixel(bitmapX, bitmapY, argb);
      }

//      Log.d("Liby", "Finishing i = " + i);
    }

//    textureBitmaps[0].setPixel(0, 0, 0xffff00ff);
//    textureBitmaps[0].setPixel(1, 0, 0xffff0000);
//    textureBitmaps[0].setPixel(1, 1, 0xffffff00);
//    textureBitmaps[0].setPixel(0, 1, 0xff00ff00);

//    Log.d("Liby", "Ending AREnv Update");
  }

  public void drawToTexture() {
//    Log.d("Liby", "Pixel value of face 0 at 0, 0: " + textureBitmaps[0].getPixel(0, 0));
//    Log.d("Liby", "Pixel value of face 0 at 0, 1: " + textureBitmaps[0].getPixel(0, 1));
//    Log.d("Liby", "Pixel value of face 0 at 1, 0: " + textureBitmaps[0].getPixel(1, 0));
//    Log.d("Liby", "Pixel value of face 0 at 1, 1: " + textureBitmaps[0].getPixel(1, 1));
    for (int i = 0; i < NUM_FACES; i++) {



//      Log.d("Liby", "Generated texture_id: " + textures[i].getTextureId());
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
