package project;

import com.jogamp.opengl.GL4;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;

public class TextureLoader {

    public static int loadTexture2D(GL4 gl, String path) {
        try {
            BufferedImage image = loadImage(path);
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = imageToRGBABuffer(image, width, height);

            IntBuffer texID = IntBuffer.allocate(1);
            gl.glGenTextures(1, texID);

            int texture = texID.get(0);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, texture);

            // slanje podataka teksture na GPU
            gl.glTexImage2D(
                GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA,
                width, height, 0,
                GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, buffer);

            // mipmapping, jer poboljsava kvalitet pri udaljenim pogledima
            gl.glGenerateMipmap(GL4.GL_TEXTURE_2D);

            // filtriranje, jer bez ovoga tekstura izgleda lose pri zumiranju
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);

            // wrapping, jer zelimo da se tekstura ponavlja
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_REPEAT);
            gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_REPEAT);

            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

            System.out.println("Tekstura ucitana: " + path);
            return texture;
        }
        catch (Exception e) {
            System.err.println("Greska pri ucitavanju teksture: " + path);
            e.printStackTrace();
            return -1;
        }
    }

    public static int loadCubemap(GL4 gl, String[] paths) {
        IntBuffer texID = IntBuffer.allocate(1);
        gl.glGenTextures(1, texID);

        int texture = texID.get(0);
        gl.glBindTexture(GL4.GL_TEXTURE_CUBE_MAP, texture);

        for (int i = 0; i < 6; i++) {
            try {
                BufferedImage image = loadImage(paths[i]);
                int width = image.getWidth();
                int height = image.getHeight();
                ByteBuffer buffer = imageToRGBABuffer(image, width, height);

                gl.glTexImage2D(
                    GL4.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL4.GL_RGBA,
                    width, height, 0,
                    GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, buffer);

                System.out.println("Cubemap strana " + i + " ucitana: " + paths[i]);
            }
            catch (Exception e) {
                System.err.println("Greska pri ucitavanju cubemap strane: " + paths[i]);
                e.printStackTrace();
            }
        }

        gl.glTexParameteri(GL4.GL_TEXTURE_CUBE_MAP, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_CUBE_MAP, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_CUBE_MAP, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL4.GL_TEXTURE_CUBE_MAP, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL4.GL_TEXTURE_CUBE_MAP, GL4.GL_TEXTURE_WRAP_R, GL4.GL_CLAMP_TO_EDGE);

        gl.glBindTexture(GL4.GL_TEXTURE_CUBE_MAP, 0);

        System.out.println("Cubemap ucitana.");
        return texture;
    }

    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        }
        catch (Exception e) {
            System.err.println("Greska pri ucitavanju slike: " + path);
            e.printStackTrace();
            return null;
        }
    }

    // uzima vrednost heightmap-e u koordinatama (u, v) i vraca visinu u opsegu [0, 1]
    public static float sampleHeightMap(BufferedImage heightMap, float u, float v) {
        int width = heightMap.getWidth();
        int height = heightMap.getHeight();

        u = Math.max(0, Math.min(1, u));
        v = Math.max(0, Math.min(1, v));

        // konvertuj (u, v) u piksel koordinate
        int x = (int)(u * (width - 1));
        int y = (int)(v * (height - 1));

        int rgb = heightMap.getRGB(x, y);

        // grayscale: r == g == b
        int b = rgb & 0xFF;
        return b / 255.0f;
    }

    private static ByteBuffer imageToRGBABuffer(BufferedImage image, int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);

                // R => G => B => A
                buffer.put((byte)((argb >> 16) & 0xFF));
                buffer.put((byte)((argb >> 8) & 0xFF));
                buffer.put((byte)(argb & 0xFF));
                buffer.put((byte)((argb >> 24) & 0xFF));
            }
        }
        
        // flip, jer Java ImageIO ucitava slike od vrha nadole, a OpenGL ocekuje od dna nagore
        buffer.flip();
        
        return buffer;
    }

}
