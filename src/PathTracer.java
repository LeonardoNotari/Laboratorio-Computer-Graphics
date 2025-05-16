package src;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.IOException;
import java.nio.*;
import java.nio.file.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glDispatchCompute;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class PathTracer {
    private int width = 1000;
    private int height = 800;
    
    // Finestra e shader
    private long window;
    private int computeShader;
    private int displayShader;

    private int outputTexture;
    private int quadVAO, quadVBO;
    private int SSBO;
    
    // Camera
    private Vector3f cameraPos = new Vector3f(0.0f, 1.3f, 3.0f);
    private Vector3f cameraTarget = new Vector3f(0.0f, 1.0f, 0.0f);
    private Matrix4f viewProjection = new Matrix4f();
    
    // Sfere
    private Sphere[] spheres = {
        // 0 : materiale diffusivo
        // 1 : materiale riflettente
        // 2 : materiale rifrattivo
        // 3 : materiale emissivo

        new Sphere(new Vector3f(0.6f, 0.4f, -0.5f), 0.4f, new Vector3f(0.1f, 0.6f, 0.1f), 2), // Sfera dx
        new Sphere(new Vector3f(-0.6f, 0.4f, -1.0f), 0.4f, new Vector3f(0.7f, 0.7f, 0.7f), 1), // Sfera sx

        new Sphere(new Vector3f(0.0f, 1001.95f, 0.0f), 1000.0f, new Vector3f(0.6f, 0.6f, 0.6f), 0), // soffitto 
        new Sphere(new Vector3f(0.0f, -1000.0f, 0.0f), 1000.0f, new Vector3f(0.6f, 0.6f, 0.6f), 0),// Pavimento
        new Sphere(new Vector3f(-1001.2f, 0.0f, 0.0f), 1000.0f, new Vector3f(0.6f, 0.1f, 0.1f), 0), // parete sx
        new Sphere(new Vector3f(1001.2f, 0.0f, 0.0f), 1000.0f, new Vector3f(0.1f, 0.1f, 0.6f), 0), // parete dx
        new Sphere(new Vector3f(0.0f, 0.0f, -1003.0f), 1000.0f, new Vector3f(0.6f, 0.6f, 0.6f), 0), // retro 
        new Sphere(new Vector3f(10.8f, 1001.9f, 0.0f), 1000.0f, new Vector3f(0.6f, 0.6f, 0.6f), 0), // copertura soffitto
        new Sphere(new Vector3f(-10.8f, 1001.9f, 0.0f), 1000.0f, new Vector3f(0.6f, 0.6f, 0.6f), 0), // copertura soffitto

        
        new Sphere(new Vector3f(0.0f, 11.942f, -0.8f), 10.0f, new Vector3f(1.0f, 1.0f, 1.0f), 3) // luce
    };
    
    private int samplesPerPixel = 32; //campioni per pixel
    private boolean sceneChange = false;
    
    class Sphere {
        Vector3f center;
        float radius;
        Vector3f albedo;
        int materialType;
        
        Sphere(Vector3f center, float radius, Vector3f albedo, int materialType) {
            this.center = center;
            this.radius = radius;
            this.albedo = albedo;
            this.materialType = materialType;
        }
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // inizializzazione della libreria GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        // creazione della finestra
        window = glfwCreateWindow(width, height, "Path Tracer", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(window, this::keyCallback);

        // Centra la finestra
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor()); // prende la dimensione del monitor

            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        // rende la finestra visibile
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); 
        glfwShowWindow(window);

        GL.createCapabilities();

        // Carica gli shader
        try {
            computeShader = ShaderUtils.createComputeShader("shaders/path_tracer.comp"); //carica il compute shader
            displayShader = ShaderUtils.createShaderProgram("shaders/display.vert", "shaders/display.frag"); //carica vertex e gragment shader
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Creazione della texture delle dimensioni dello schermo
        outputTexture = createOutputTexture(width, height);

        // Crea e carica lo SSBO per le sfere e la stanza
        SSBO = createSSBO();

        // Setup del quad per il rendering
        setupQuad();

        // Setup della camera
        updateCamera();
    }

    private int createOutputTexture(int width, int height) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture); // associa la texture (le chiamate glTex* si applicheranno a questa texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); //wrap verticale
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); //wrap orizzontale
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL_FLOAT, 0); 
        return texture;
    }

    private int createSSBO() {
        // 8 float per sfera (center(3) + radius(1), albedo(3) + materialType(1))
        FloatBuffer buffer = BufferUtils.createFloatBuffer(spheres.length * 8);

        for (Sphere sphere : spheres) {
            buffer.put(sphere.center.x).put(sphere.center.y).put(sphere.center.z);
            buffer.put(sphere.radius);
            buffer.put(sphere.albedo.x).put(sphere.albedo.y).put(sphere.albedo.z);
            buffer.put(sphere.materialType);
        }
        
        buffer.flip();
        
        int ssbo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_STATIC_DRAW); //alloca dati nella gpu
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssbo); //binding al binding point 1
    
        return ssbo;
    }

    private void setupQuad() {
        float[] quadVertices = {
            -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
             1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
             1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        };
        
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
    }

    private void updateCamera() {
        Matrix4f view = new Matrix4f()
            .lookAt(cameraPos, cameraTarget, new Vector3f(0.0f, 1.0f, 0.0f));
        
        Matrix4f projection = new Matrix4f()
            .perspective((float)Math.toRadians(45.0f), (float)width/height, 0.1f, 100.0f);
        
        viewProjection = projection.mul(view, new Matrix4f());
    }

    private void loop() {
        render();
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            if(sceneChange){
                render();
                sceneChange = false;
            }
        }
    }

    private void render() {
        glUseProgram(computeShader);
        
        glUniform3f(glGetUniformLocation(computeShader, "cameraPos"), cameraPos.x, cameraPos.y, cameraPos.z);
        glUniform1i(glGetUniformLocation(computeShader, "samples"), samplesPerPixel);
        glUniform1i(glGetUniformLocation(computeShader, "sphereCount"), spheres.length);

        //inverto la matrice di proiezione per usarla nel compute shader
        try (MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            viewProjection.invert().get(fb);
            glUniformMatrix4fv(glGetUniformLocation(computeShader, "invViewProj"), false, fb);
        }
        

        glBindImageTexture(0, outputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F); //binding al binding point 0
        
        // calcola il numero di working group ed esegue il compute shader
        int workGroupSize = 16;
        int workGroupsX = (width + workGroupSize - 1) / workGroupSize;
        int workGroupsY = (height + workGroupSize - 1) / workGroupSize;
        glDispatchCompute(workGroupsX, workGroupsY, 1);

        //barriera per aspettare che tutti i thread abbiano concluso prima di andare avanti
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        // Visualizza il risultato
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(displayShader);
        glBindVertexArray(quadVAO);
        glBindTexture(GL_TEXTURE_2D, outputTexture);
        glActiveTexture(GL_TEXTURE0);
        glUniform1i(glGetUniformLocation(displayShader, "screenTexture"), 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        glfwSwapBuffers(window);
    }
  

    private void cleanup() {
        glDeleteProgram(computeShader);
        glDeleteProgram(displayShader);
        glDeleteTextures(outputTexture);
        glDeleteBuffers(SSBO);
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(window, true);
        }
        
        float cameraSpeed = 0.1f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(0.0f, 0.0f, -cameraSpeed));
            sceneChange = true;
            updateCamera();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(0.0f, 0.0f, cameraSpeed));
            sceneChange = true;
            updateCamera();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(-cameraSpeed, 0.0f, 0.0f));
            sceneChange = true;
            updateCamera();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            cameraPos.add(new Vector3f(cameraSpeed, 0.0f, 0.0f));
            sceneChange = true;
            updateCamera();
        }

        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(window, true);
        }
    }

    public static void main(String[] args) {
        new PathTracer().run();
    }
}

class ShaderUtils {
    public static int createShaderProgram(String vertexPath, String fragmentPath) throws IOException {
        int vertexShader = loadShader(vertexPath, GL_VERTEX_SHADER);
        int fragmentShader = loadShader(fragmentPath, GL_FRAGMENT_SHADER);
        
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        
        checkProgramLink(program);
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        return program;
    }
    
    public static int createComputeShader(String computePath) throws IOException {
        int computeShader = loadShader(computePath, GL_COMPUTE_SHADER);
        
        int program = glCreateProgram();
        glAttachShader(program, computeShader);
        glLinkProgram(program);
        
        checkProgramLink(program);
        
        glDeleteShader(computeShader);
        
        return program;
    }
    
    private static int loadShader(String filePath, int type) throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Errore nella compilazione dello shader " + filePath + ":");
            System.err.println(glGetShaderInfoLog(shader));
            System.exit(1);
        }
        
        return shader;
    }
    
    private static void checkProgramLink(int program) {
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Errore nel linking del programma shader:");
            System.err.println(glGetProgramInfoLog(program));
            System.exit(1);
        }
    }
}








