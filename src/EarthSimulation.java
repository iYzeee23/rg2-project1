package project;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.FPSAnimator;
import org.joml.Vector3f;

import java.nio.file.Paths;

public class EarthSimulation implements GLEventListener, KeyListener, MouseListener {

    private static final String BASE_DIR = resolveBaseDir();
    private static final String ASSETS_PATH = BASE_DIR + "assets/";

    private Camera camera;
    private CubeSphere earth;
    private Skybox skybox;

    private final Vector3f lightPosition = new Vector3f(5.0f, 3.0f, 5.0f);

    // R = rotacija Zemlje, L = kruzenje Sunca
    private boolean earthRotating = false;
    private boolean lightOrbiting = false;
    private float earthAngle = 0.0f;
    private float lightAngle = 0.0f;
    private static final float EARTH_SPEED = 0.3f;
    private static final float LIGHT_SPEED = 0.5f;

    private GLWindow window;
    private FPSAnimator animator;

    private boolean leftMouseDown = false;
    private int lastMouseX, lastMouseY;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        // uvek palimo depth test na pocetku
        gl.glEnable(GL4.GL_DEPTH_TEST);

        // za skybox, dubina 1.0 mora da prodje test
        gl.glDepthFunc(GL4.GL_LEQUAL);

        // bez ovoga vidljivi su savovi izmedju strana cubemap-a
        gl.glEnable(GL4.GL_TEXTURE_CUBE_MAP_SEAMLESS);

        // CCW namotaj je front, zadnja strana se ne crta
        gl.glEnable(GL4.GL_CULL_FACE);

        // ocisti boju u color bufferu
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        camera = new Camera();

        String shadersPath = BASE_DIR + "shaders/";
        earth = new CubeSphere(64, 1.0f, 0.03f, ASSETS_PATH, shadersPath);
        earth.init(gl);

        skybox = new Skybox(ASSETS_PATH + "skybox/", shadersPath);
        skybox.init(gl);

        System.out.println("*** Inicijalizacija zavrsena ***");
        printHelp();
    }

    private void printHelp() {
        System.out.println("\n*** Kontrole ***");
        System.out.println("  Scroll: Zoom");
        System.out.println("  Levi klik + drag: Rotacija kamere");
        System.out.println("  R: Rotacija Zemlje ON/OFF");
        System.out.println("  L: Kruzenje Sunca ON/OFF");
        System.out.println("  F1: Prikazi ovu pomoc");
        System.out.println("  ESC: Izlaz");
        System.out.println("****************\n");
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        // pretpostavljamo 60 FPS
        float dt = 1.0f / 60.0f;
        if (earthRotating) earthAngle += EARTH_SPEED * dt;
        if (lightOrbiting) lightAngle += LIGHT_SPEED * dt;

        // svetlo kruzi oko Y ose, na istoj udaljenosti kao originalna pozicija
        float lightRadius = (float)Math.sqrt(5*5 + 5*5);
        Vector3f currentLightPos = new Vector3f(
            lightRadius * (float)Math.sin(lightAngle),
            3.0f,
            lightRadius * (float)Math.cos(lightAngle)
        );

        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        // skybox prvo (iza svega), pa Zemlja
        skybox.render(gl, camera);
        earth.render(gl, camera, currentLightPos, earthAngle);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);

        if (height > 0) {
            camera.setAspectRatio((float) width / height);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        earth.destroy(gl);
        skybox.destroy(gl);
        System.out.println("Resursi oslobodjeni.");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (animator != null && animator.isAnimating()) {
                animator.stop();
            }
            if (window != null) {
                window.destroy();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            earthRotating = !earthRotating;
        }
        if (e.getKeyCode() == KeyEvent.VK_L) {
            lightOrbiting = !lightOrbiting;
        }
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            printHelp();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMouseDown = true;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMouseDown = false;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        if (leftMouseDown) {
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;

            camera.rotateHorizontal(dx);
            camera.rotateVertical(-dy);

            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        float[] rotation = e.getRotation();

        // rotation[1] je vertikalna rotacija tocka misa
        camera.zoom(rotation[1]);
    }

    // trazi assets/ folder relativno od working directory-ja
    // podrzava pokretanje i iz project foldera i iz roditeljskog
    public static String resolveBaseDir() {
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString().replace('\\', '/') + "/";
        if (new java.io.File(cwd + "assets").exists()) return cwd;
        if (new java.io.File(cwd + "project/assets").exists()) return cwd + "project/";

        System.out.println("UPOZORENJE: assets folder nije pronadjen!");
        System.out.println("Trenutni direktorijum: " + cwd);
        System.out.println("Pokrenite program iz project/ foldera.");
        return cwd;
    }

    public static void main(String[] args) {
        // GL4 = programabilni pipeline
        GLProfile profile = GLProfile.getMaxProgrammable(true);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);
        caps.setDoubleBuffered(true);

        GLWindow window = GLWindow.create(caps);
        window.setSize(1024, 768);
        window.setTitle("Simulacija planete Zemlje - RG2 Domaci");

        EarthSimulation sim = new EarthSimulation();
        sim.window = window;

        window.addGLEventListener(sim);
        window.addKeyListener(sim);
        window.addMouseListener(sim);

        FPSAnimator animator = new FPSAnimator(window, 60);
        sim.animator = animator;
        animator.start();

        window.setVisible(true);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                // animator mora da stane pre nego sto se prozor unisti
                if (animator.isAnimating()) {
                    animator.stop();
                }
            }

            @Override
            public void windowDestroyed(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
