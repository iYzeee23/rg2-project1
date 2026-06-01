package project;

import com.jogamp.opengl.GL4;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderUtils {

    public static int compileShader(GL4 gl, int type, String source) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[]{source}, null);
        gl.glCompileShader(shader);

        return shader;
    }

    public static int createProgram(GL4 gl, int vertexShader, int fragmentShader) {
        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);

        // shaderi se mogu obrisati nakon linkovanja
        // program ih cuva interno
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        return program;
    }

    public static String loadShaderSource(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        }
        catch (IOException e) {
            System.err.println("Greska pri ucitavanju shader fajla: " + path);
            e.printStackTrace();
            return "";
        }
    }

    public static int getUniformLocation(GL4 gl, int program, String name) {
        return gl.glGetUniformLocation(program, name);
    }

}
