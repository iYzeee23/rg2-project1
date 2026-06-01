package project;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class CubeSphere {
    private final int resolution;
    private final float radius;
    private final float heightScale;
    private final String assetsPath;

    private int vao;
    private int positionVBO, uvVBO, normalVBO, tangentVBO;
    private int indexBuffer;
    private int indexCount;

    private int earthTexture;
    private int normalMapTexture;
    private int specularMapTexture;

    private int shaderProgram;
    private int mvpLoc, mvLoc, ntLoc, lightPosLoc;
    private int earthTexLoc, normalMapLoc, specularMapLoc;

    private final float[] mat4Array = new float[16];
    private final float[] mat3Array = new float[9];

    private String shadersPath;

    // https://wikis.khronos.org/opengl/Cubemap_Texture
    private static final float[][] FACE_ORIGINS = {
        { -1, -1,  1 }, // +Z
        {  1, -1, -1 }, // -Z
        {  1, -1,  1 }, // +X
        { -1, -1, -1 }, // -X
        { -1,  1,  1 }, // +Y
        { -1, -1, -1 }, // -Y
    };
    private static final float[][] FACE_RIGHTS = {
        {  2,  0,  0 }, // +Z
        { -2,  0,  0 }, // -Z
        {  0,  0, -2 }, // +X
        {  0,  0,  2 }, // -X
        {  2,  0,  0 }, // +Y
        {  2,  0,  0 }, // -Y
    };
    private static final float[][] FACE_UPS = {
        {  0,  2,  0 }, // +Z
        {  0,  2,  0 }, // -Z
        {  0,  2,  0 }, // +X
        {  0,  2,  0 }, // -X
        {  0,  0, -2 }, // +Y
        {  0,  0,  2 }, // -Y
    };

    public CubeSphere(int resolution, float radius, float heightScale, String assetsPath, String shadersPath) {
        this.resolution = resolution;
        this.radius = radius;
        this.heightScale = heightScale;
        this.assetsPath = assetsPath;
        this.shadersPath = shadersPath;
    }

    public void init(GL4 gl) {
        generateGeometry(gl);
        loadTextures(gl);
        buildShader(gl);
    }

    private void generateGeometry(GL4 gl) {
        BufferedImage heightMap = TextureLoader.loadImage(assetsPath + "heightMap.jpg");

        ArrayList<Float> positions = new ArrayList<>();
        ArrayList<Float> uvs = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Float> tangents = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();

        int verticesPerFace = (resolution + 1) * (resolution + 1);

        for (int face = 0; face < 6; face++) {
            float ox = FACE_ORIGINS[face][0];
            float oy = FACE_ORIGINS[face][1];
            float oz = FACE_ORIGINS[face][2];

            float rx = FACE_RIGHTS[face][0];
            float ry = FACE_RIGHTS[face][1];
            float rz = FACE_RIGHTS[face][2];

            float ux = FACE_UPS[face][0];
            float uy = FACE_UPS[face][1];
            float uz = FACE_UPS[face][2];

            for (int i = 0; i <= resolution; i++) {
                for (int j = 0; j <= resolution; j++) {
                    float fi = (float)i / resolution;
                    float fj = (float)j / resolution;

                    float cx = ox + fj * rx + fi * ux;
                    float cy = oy + fj * ry + fi * uy;
                    float cz = oz + fj * rz + fi * uz;

                    float len = (float)Math.sqrt(cx * cx + cy * cy + cz * cz);
                    float nx = cx / len;
                    float ny = cy / len;
                    float nz = cz / len;

                    // ove formule su date u postavci zadatka
                    // sfera je u centru koordinatnog sistema
                    // zbog toga je cx = cy = cz = 0
                    float theta = (float)Math.atan2(-nz, nx);
                    float phi = (float)Math.acos(Math.max(-1, Math.min(1, -ny)));
                    float u = (float)((theta + Math.PI) / (2 * Math.PI));
                    float v = (float)(phi / Math.PI);

                    float h = TextureLoader.sampleHeightMap(heightMap, u, v);
                    float r = radius + h * heightScale;

                    float px = nx * r;
                    float py = ny * r;
                    float pz = nz * r;

                    positions.add(px);
                    positions.add(py);
                    positions.add(pz);

                    uvs.add(u);
                    uvs.add(v);

                    normals.add(nx);
                    normals.add(ny);
                    normals.add(nz);

                    // posto imamo normalnu, potrebna nam je tangenta/bitangenta za TBN matricu
                    // posto shader koristi normalu i tangentu, ovde cemo da formiramo tangentu
                    // bitangentnu mozemo da izracunamo u shaderu kao cross product normale i tangente

                    // na sferi, pozicija tacke je:
                    // x = cos(theta)
                    // y = -cos(phi)
                    // z = -sin(theta)

                    // tangenta je izvod pozicije po theti, jer theta kontrolise kretanje oko Y ose
                    // tx = -sin(theta)
                    // ty = 0
                    // tz = -cos(theta)

                    // https://www.opengl-tutorial.org/intermediate-tutorials/tutorial-13-normal-mapping/
                    // https://mathworld.wolfram.com/SphericalCoordinates.html
                    float sinTheta = (float)Math.sin(theta);
                    float cosTheta = (float)Math.cos(theta);
                    float tx = -sinTheta;
                    float ty = 0;
                    float tz = -cosTheta;
                    
                    float tLen = (float)Math.sqrt(tx*tx + ty*ty + tz*tz);
                    if (tLen > 0.0001f) {
                        tx /= tLen;
                        ty /= tLen;
                        tz /= tLen;
                    } 
                    else {
                        tx = 1;
                        ty = 0;
                        tz = 0;
                    }

                    tangents.add(tx);
                    tangents.add(ty);
                    tangents.add(tz);
                }
            }

            // svaka strana ima svoje numerisane vertekse
            int baseVertex = face * verticesPerFace;

            // v0 --- v1
            // |    /  |
            // |   /   |
            // |  /    |
            // v2 --- v3

            // 0 --- 1 --- 2 --- 3
            // |     |     |     |
            // 4 --- 5 --- 6 --- 7
            // |     |     |     |
            // 8 --- 9 --- 10--- 11
            // |     |     |     |
            // 12--- 13--- 14--- 15

            for (int i = 0; i < resolution; i++) {
                for (int j = 0; j < resolution; j++) {
                    int v0 = baseVertex + i * (resolution + 1) + j;
                    int v1 = v0 + 1;
                    int v2 = v0 + (resolution + 1);
                    int v3 = v2 + 1;

                    // crtezi iznad su iz perspektive centra sfere
                    // dakle, za kameru ce ovo biti CCW, kao sto i ocekujemo
                    indices.add(v0); indices.add(v1); indices.add(v2);
                    indices.add(v1); indices.add(v3); indices.add(v2);
                }
            }
        }

        // koordinata "u" ide od 0 do 1 oko sfere, pa dolazi do problema na savovima
        // naime, dva susedna verteksa imaju u=0.98 i u=0.02 umesto u=0.98 i u=1.02
        // zato, GPU interpolira 0.98 -> 0.02 unazad kroz celu teksturu

        // za problematicne trouglove radimo sledece:
        // KOPIRAMO vertex sa malim u i postavimo mu u += 1.0 [dakle, novi vertex]
        // MENJAMO postojece problematicne trouglove [dakle, isti trougao]

        // na ovaj nacin, stari vertex ostaje za trouglove kojima odgovara
        // s druge strane, samo problematicni trougao pokazuje na ispravljenu kopiju

        fixUVSeam(positions, uvs, normals, tangents, indices);

        indexCount = indices.size();
        System.out.println("CubeSphere: " + (positions.size()/3) + " verteksa, " + (indexCount/3) + " trouglova");

        uploadToGPU(gl, positions, uvs, normals, tangents, indices);
    }

    // https://stackoverflow.com/questions/9511499/seam-issue-when-mapping-a-texture-to-a-sphere-in-opengl
    private void fixUVSeam(
        ArrayList<Float> positions,
        ArrayList<Float> uvs,
        ArrayList<Float> normals,
        ArrayList<Float> tangents,
        ArrayList<Integer> indices
    ) {
        int triCount = indices.size() / 3;

        for (int t = 0; t < triCount; t++) {
            int i0 = indices.get(t * 3);
            int i1 = indices.get(t * 3 + 1);
            int i2 = indices.get(t * 3 + 2);

            float u0 = uvs.get(i0 * 2);
            float u1 = uvs.get(i1 * 2);
            float u2 = uvs.get(i2 * 2);

            float maxU = Math.max(u0, Math.max(u1, u2));
            float minU = Math.min(u0, Math.min(u1, u2));

            if (maxU - minU > 0.5f) {
                if (u0 < 0.3f) {
                    int newIdx = duplicateVertex(positions, uvs, normals, tangents, i0, 1.0f);
                    indices.set(t * 3, newIdx);
                }
                if (u1 < 0.3f) {
                    int newIdx = duplicateVertex(positions, uvs, normals, tangents, i1, 1.0f);
                    indices.set(t * 3 + 1, newIdx);
                }
                if (u2 < 0.3f) {
                    int newIdx = duplicateVertex(positions, uvs, normals, tangents, i2, 1.0f);
                    indices.set(t * 3 + 2, newIdx);
                }
            }
        }
    }

    // kreira novi vertex sa svim postojecim atributima atributima, osim UV
    // dakle, pozicija, normala i tangenta ostaju iste, ali se UV menja
    private int duplicateVertex(
        ArrayList<Float> positions,
        ArrayList<Float> uvs,
        ArrayList<Float> normals,
        ArrayList<Float> tangents,
        int srcIdx,
        float uOffset
    ) {
        int newIdx = positions.size() / 3;

        positions.add(positions.get(srcIdx * 3));
        positions.add(positions.get(srcIdx * 3 + 1));
        positions.add(positions.get(srcIdx * 3 + 2));

        uvs.add(uvs.get(srcIdx * 2) + uOffset);
        uvs.add(uvs.get(srcIdx * 2 + 1));

        normals.add(normals.get(srcIdx * 3));
        normals.add(normals.get(srcIdx * 3 + 1));
        normals.add(normals.get(srcIdx * 3 + 2));

        tangents.add(tangents.get(srcIdx * 3));
        tangents.add(tangents.get(srcIdx * 3 + 1));
        tangents.add(tangents.get(srcIdx * 3 + 2));

        return newIdx;
    }

    private void uploadToGPU(
        GL4 gl,
        ArrayList<Float> positions,
        ArrayList<Float> uvs,
        ArrayList<Float> normals,
        ArrayList<Float> tangents,
        ArrayList<Integer> indices
    ) {
        float[] posArray = toFloatArray(positions);
        float[] uvArray  = toFloatArray(uvs);
        float[] norArray = toFloatArray(normals);
        float[] tanArray = toFloatArray(tangents);
        int[] idxArray = toIntArray(indices);

        // vao
        IntBuffer intBuf = IntBuffer.allocate(1);
        gl.glGenVertexArrays(1, intBuf);
        vao = intBuf.get(0);
        gl.glBindVertexArray(vao);

        // vbo za lokaciju 0 (pozicija)
        positionVBO = createVBO(gl, posArray);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // vbo za lokaciju 1 (uv koordinate)
        uvVBO = createVBO(gl, uvArray);
        gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        // vbo za lokaciju 2 (normale)
        normalVBO = createVBO(gl, norArray);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        // vbo za lokaciju 3 (tangente)
        tangentVBO = createVBO(gl, tanArray);
        gl.glVertexAttribPointer(3, 3, GL4.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(3);

        // index buffer
        IntBuffer idxIntBuf = IntBuffer.allocate(1);
        gl.glGenBuffers(1, idxIntBuf);
        indexBuffer = idxIntBuf.get(0);
        gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        IntBuffer idxData = Buffers.newDirectIntBuffer(idxArray);
        gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, (long) idxArray.length * Integer.BYTES, idxData, GL4.GL_STATIC_DRAW);

        gl.glBindVertexArray(0);
    }
    
    private int createVBO(GL4 gl, float[] data) {
        IntBuffer buf = IntBuffer.allocate(1);
        gl.glGenBuffers(1, buf);
        int vbo = buf.get(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = Buffers.newDirectFloatBuffer(data);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) data.length * Float.BYTES, fb, GL4.GL_STATIC_DRAW);
        return vbo;
    }

    private void loadTextures(GL4 gl) {
        earthTexture = TextureLoader.loadTexture2D(gl, assetsPath + "earth.jpg");
        normalMapTexture = TextureLoader.loadTexture2D(gl, assetsPath + "normalMap.jpg");
        specularMapTexture = TextureLoader.loadTexture2D(gl, assetsPath + "specularMap.jpg");
    }

    private void buildShader(GL4 gl) {
        String vsSrc = ShaderUtils.loadShaderSource(shadersPath + "earth_vertex.glsl");
        String fsSrc = ShaderUtils.loadShaderSource(shadersPath + "earth_fragment.glsl");
        int vs = ShaderUtils.compileShader(gl, GL4.GL_VERTEX_SHADER, vsSrc);
        int fs = ShaderUtils.compileShader(gl, GL4.GL_FRAGMENT_SHADER, fsSrc);
        shaderProgram = ShaderUtils.createProgram(gl, vs, fs);

        mvpLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "MVPTransform");
        mvLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "MVTransform");
        ntLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "NormalTransform");
        lightPosLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "LightPosition");
        earthTexLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "earthTexture");
        normalMapLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "normalMap");
        specularMapLoc = ShaderUtils.getUniformLocation(gl, shaderProgram, "specularMap");
    }

    public void render(GL4 gl, Camera camera, Vector3f lightPosition, float earthAngle) {
        gl.glUseProgram(shaderProgram);

        // rotacija zemlje oko Y ose, pa je eksplicitno potrebna model matrica
        Matrix4f modelMatrix = new Matrix4f().rotateY(earthAngle);

        Matrix4f mvpMatrix = new Matrix4f()
            .mul(camera.getProjectionMatrix())
            .mul(camera.getViewMatrix())
            .mul(modelMatrix);
        mvpMatrix.get(mat4Array);
        gl.glUniformMatrix4fv(mvpLoc, 1, false, mat4Array, 0);

        Matrix4f mvMatrix = new Matrix4f()
            .mul(camera.getViewMatrix())
            .mul(modelMatrix);
        mvMatrix.get(mat4Array);
        gl.glUniformMatrix4fv(mvLoc, 1, false, mat4Array, 0);

        // potrebna za TBN matricu
        Matrix3f normalTransform = new Matrix3f();
        mvMatrix.get3x3(normalTransform);
        normalTransform.invert().transpose();
        normalTransform.get(mat3Array);
        gl.glUniformMatrix3fv(ntLoc, 1, false, mat3Array, 0);

        // transformacija svetla u prostoru kamere koristeci samo view matricu
        // svetlo je u prostoru sveta i ne treba da rotira sa zemljom
        Matrix4f viewMatrix = camera.getViewMatrix();
        Vector3f lightEye = new Vector3f();
        viewMatrix.transformPosition(lightPosition, lightEye);
        gl.glUniform3f(lightPosLoc, lightEye.x, lightEye.y, lightEye.z);

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, earthTexture);
        gl.glUniform1i(earthTexLoc, 0);

        gl.glActiveTexture(GL4.GL_TEXTURE1);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, normalMapTexture);
        gl.glUniform1i(normalMapLoc, 1);

        gl.glActiveTexture(GL4.GL_TEXTURE2);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, specularMapTexture);
        gl.glUniform1i(specularMapLoc, 2);

        gl.glBindVertexArray(vao);
        gl.glDrawElements(GL4.GL_TRIANGLES, indexCount, GL4.GL_UNSIGNED_INT, 0);
        gl.glBindVertexArray(0);
    }

    public void destroy(GL4 gl) {
        gl.glDeleteVertexArrays(1, IntBuffer.wrap(new int[]{vao}));
        gl.glDeleteBuffers(4, IntBuffer.wrap(new int[]{positionVBO, uvVBO, normalVBO, tangentVBO}));
        gl.glDeleteBuffers(1, IntBuffer.wrap(new int[]{indexBuffer}));
        gl.glDeleteTextures(3, IntBuffer.wrap(new int[]{earthTexture, normalMapTexture, specularMapTexture}));
        gl.glDeleteProgram(shaderProgram);
    }

    private static float[] toFloatArray(ArrayList<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] toIntArray(ArrayList<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}
