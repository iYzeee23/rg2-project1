package project;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    // azimuth i elevation su uglovi u sfernim koordinatama
    // distance je rastojanje od centra
    private float azimuth = 0.0f;
    private float elevation = 0.3f;
    private float distance = 3.0f;

    private static final float MIN_DISTANCE = 1.2f;
    private static final float MAX_DISTANCE = 10.0f;
    private static final float MIN_ELEVATION = (float)Math.toRadians(-89.0);
    private static final float MAX_ELEVATION = (float)Math.toRadians(89.0);

    private static final float ROTATION_SENSITIVITY = 0.005f;
    private static final float ZOOM_SENSITIVITY = 0.1f;

    // field of view je ugao vidnog polja u vertikalnoj ravni
    // aspect ratio je sirina podeljena visinom prozora
    private float fov = (float)Math.toRadians(45.0);
    private float aspectRatio = 1.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 100.0f;

    private Vector3f position = new Vector3f();

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public void rotateHorizontal(float deltaX) {
        azimuth += deltaX * ROTATION_SENSITIVITY;
    }

    public void rotateVertical(float deltaY) {
        elevation += deltaY * ROTATION_SENSITIVITY;
        elevation = Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, elevation));
    }

    public void zoom(float delta) {
        distance -= delta * ZOOM_SENSITIVITY;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    // https://www.mathworks.com/help/simulink/ref_extras/sphericaltocartesian.html
    // phi + elevation == 90 => elevation == 90 - phi
    // theta + azimuth == 90 => azimuth == 90 - theta
    // sin(90 - angle) = cos(angle)
    // cos(90 - angle) = sin(angle)

    private void updatePosition() {
        float cosElev = (float)Math.cos(elevation);
        float sinElev = (float)Math.sin(elevation);
        float cosAzim = (float)Math.cos(azimuth);
        float sinAzim = (float)Math.sin(azimuth);

        float x = distance * cosElev * sinAzim;
        float y = distance * sinElev;
        float z = distance * cosElev * cosAzim;

        position.set(x, y, z);
    }

    public Matrix4f getViewMatrix() {
        updatePosition();

        Vector3f eye = position;
        Vector3f center = new Vector3f(0, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);

        return new Matrix4f().lookAt(eye, center, up);
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective(fov, aspectRatio, nearPlane, farPlane);
    }

    // mnozenje ide u obrnutom redosledu
    // prvo view, pa projection
    public Matrix4f getViewProjection() {
        return new Matrix4f()
            .mul(getProjectionMatrix())
            .mul(getViewMatrix());
    }

    public Vector3f getPosition() {
        updatePosition();

        return new Vector3f(position);
    }
    
}
