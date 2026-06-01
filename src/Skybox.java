package project;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Skybox {

    private final String skyboxPath;
    private final String shadersPath;

    // opengl resursi
    private int vao;
    private int vbo;
    private int cubemapTexture;
    private int shaderProgram;
    private int vpLoc, skyboxTexLoc;

    // pomocni niz za slanje matrica na GPU
    private final float[] mat4Array = new float[16];

    // 36 verteksa = 6 strana × 2 trougla × 3 verteksa
    // opengl => counter clock wise
    private static final float[] CUBE_VERTICES = {
        // Zadnja strana (-Z)
        -1,  1, -1,     -1, -1, -1,      1, -1, -1,
         1, -1, -1,      1,  1, -1,     -1,  1, -1,
        // Prednja strana (+Z)
        -1, -1,  1,     -1,  1,  1,      1,  1,  1,
         1,  1,  1,      1, -1,  1,     -1, -1,  1,
        // Leva strana (-X)
        -1,  1,  1,     -1,  1, -1,     -1, -1, -1,
        -1, -1, -1,     -1, -1,  1,     -1,  1,  1,
        // Desna strana (+X)
         1,  1,  1,      1, -1,  1,      1, -1, -1,
         1, -1, -1,      1,  1, -1,      1,  1,  1,
        // Donja strana (-Y)
        -1, -1, -1,     -1, -1,  1,      1, -1,  1,
         1, -1,  1,      1, -1, -1,     -1, -1, -1,
        // Gornja strana (+Y)
        -1,  1, -1,      1,  1, -1,      1,  1,  1,
         1,  1,  1,     -1,  1,  1,     -1,  1, -1,
    };

    public Skybox(String skyboxPath, String shadersPath) {
        this.skyboxPath = skyboxPath;
        this.shadersPath = shadersPath;
    }

    public void init(GL4 gl) {
        buildGeometry(gl);
        loadCubemap(gl);
        buildShader(gl);
    }

    private void buildGeometry(GL4 gl) {
        // vao
        IntBuffer intBuf = IntBuffer.allocate(1);
        gl.glGenVertexArrays(1, intBuf);
        vao = intBuf.get(0);
        gl.glBindVertexArray(vao);

        // vbo
        intBuf.rewind();
        gl.glGenBuffers(1, intBuf);
        vbo = intBuf.get(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = Buffers.newDirectFloatBuffer(CUBE_VERTICES);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)CUBE_VERTICES.length * Float.BYTES, fb, GL4.GL_STATIC_DRAW);

        // govori GPU kako da cita podatke iz VBO
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        
        // ukljucuje atribut na lokaciji 0
        gl.glEnableVertexAttribArray(0);

        // promene VBO vise ne uticu na ovaj VAO
        gl.glBindVertexArray(0);
    }

    private void loadCubemap(GL4 gl) {
        // opengl cubemap redosled: +X, -X, +Y, -Y, +Z, -Z
        String[] faces = {
            skyboxPath + "right.jpg",
            skyboxPath + "left.jpg",
            skyboxPath + "top.jpg",
            skyboxPath + "bottom.jpg",
            skyboxPath + "front.jpg",
            skyboxPath + "back.jpg",
        };

        cubemapTexture = TextureLoader.loadCubemap(gl, faces);
    }

    private void buildShader(GL4 gl) {
        String vsSrc = ShaderUtils.loadShaderSource(shadersPath + "skybox_vertex.glsl");
        String fsSrc = ShaderUtils.loadShaderSource(shadersPath + "skybox_fragment.glsl");
        int vs = ShaderUtils.compileShader(gl, GL4.GL_VERTEX_SHADER, vsSrc);
        int fs = ShaderUtils.compileShader(gl, GL4.GL_FRAGMENT_SHADER, fsSrc);
        shaderProgram = ShaderUtils.createProgram(gl, vs, fs);

        vpLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "VPTransform");
        skyboxTexLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "skyboxTexture");
    }

    public void render(GL4 gl, Camera camera) {
        // skybox je uvek najdalje
        gl.glDepthMask(false);
        
        // iskljuci culling jer smo UNUTAR kocke (vidimo unutrasnju stranu)
        gl.glDisable(GL4.GL_CULL_FACE);

        gl.glUseProgram(shaderProgram);

        // skybox ne prati kameru, jer je beskonacno daleko
        // kada zumiramo, zelimo da ostane na istoj daljini
        Matrix4f view = camera.getViewMatrix();
        view.m30(0); view.m31(0); view.m32(0);

        // posalji VP matricu na GPU
        Matrix4f vp = new Matrix4f().mul(camera.getProjectionMatrix()).mul(view);
        vp.get(mat4Array);
        gl.glUniformMatrix4fv(vpLoc, 1, false, mat4Array, 0);

        // povezi cubemap teksturu
        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_CUBE_MAP, cubemapTexture);
        gl.glUniform1i(skyboxTexLoc, 0);

        // crtaj kocku
        gl.glBindVertexArray(vao);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 36);
        gl.glBindVertexArray(0);

        // vrati upisivanje dubine i culling
        gl.glDepthMask(true);
        gl.glEnable(GL4.GL_CULL_FACE);
    }

    public void destroy(GL4 gl) {
        gl.glDeleteVertexArrays(1, IntBuffer.wrap(new int[]{vao}));
        gl.glDeleteBuffers(1, IntBuffer.wrap(new int[]{vbo}));
        gl.glDeleteTextures(1, IntBuffer.wrap(new int[]{cubemapTexture}));
        gl.glDeleteProgram(shaderProgram);
    }
}
