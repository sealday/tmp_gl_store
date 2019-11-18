package comp3170.ass3;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import comp3170.GLException;
import comp3170.InputManager;
import comp3170.SceneObject;
import comp3170.Shader;
import comp3170.ass3.sceneobjects.Axes;
import comp3170.ass3.sceneobjects.HeightMap;
import comp3170.ass3.sceneobjects.Light;
import comp3170.ass3.sceneobjects.Line;
import comp3170.ass3.sceneobjects.Plane;
import comp3170.ass3.sceneobjects.Water;

public class Assignment3 extends JFrame implements GLEventListener {

	private static final long serialVersionUID = 7187230984213937447L;

	final private float TAU = (float) (Math.PI * 2);

	private JSONObject level;
	private GLCanvas canvas;

	final private File TEXTURE_DIRECTORY = new File("src/comp3170/ass3/textures");
	final private String GRASS_TEXTURE = "grass.jpg";
	private int grassTexture;


	final private File SHADER_DIRECTORY = new File("src/comp3170/ass3/shaders");

	private Shader simpleShader;
	final private String SIMPLE_VERTEX_SHADER = "simpleVertex.glsl";
	final private String SIMPLE_FRAGMENT_SHADER = "simpleFragment.glsl";

	private Shader colourShader;
	final private String COLOUR_VERTEX_SHADER = "coloursVertex.glsl";
	final private String COLOUR_FRAGMENT_SHADER = "coloursFragment.glsl";

	private Shader mapShader;
	final private String MAP_VERTEX_SHADER = "mapVertex.glsl";
	final private String MAP_FRAGMENT_SHADER = "mapFragment.glsl";

	private Shader waterShader;
	final private String WATER_VERTEX_SHADER = "waterVertex.glsl";
	final private String WATER_FRAGMENT_SHADER = "waterFragment.glsl";


	private Matrix4f mvpMatrix;
	private Matrix4f viewMatrix;
	private Matrix4f projectionMatrix;
	private Matrix4f lightMatrix;


	private int screenWidth = 1000;
	private int screenHeight = 1000;

	private InputManager input;
	private Animator animator;
	private long oldTime;

	private SceneObject root;
	private SceneObject camera;
	private SceneObject cameraPivot;
	private SceneObject lightPivot;
	private Light light;
	private HeightMap map;
	private Water water;
	private Line line;

	public Assignment3(JSONObject level) {
		super(level.getString("name"));
		this.level = level;


		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setSampleBuffers(true);
		capabilities.setNumSamples(4);

		this.canvas = new GLCanvas(capabilities);
		this.canvas.addGLEventListener(this);
		this.add(canvas);

		this.animator = new Animator(canvas);
		this.animator.start();
		this.oldTime = System.currentTimeMillis();


		this.input = new InputManager();
		input.addListener(this);
		input.addListener(this.canvas);


		this.setSize(screenWidth, screenHeight);
		this.setVisible(true);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();


		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);


		this.simpleShader = loadShader(SIMPLE_VERTEX_SHADER, SIMPLE_FRAGMENT_SHADER);
		this.colourShader = loadShader(COLOUR_VERTEX_SHADER, COLOUR_FRAGMENT_SHADER);
		this.mapShader = loadShader(MAP_VERTEX_SHADER, MAP_FRAGMENT_SHADER);
		this.waterShader = loadShader(WATER_VERTEX_SHADER, WATER_FRAGMENT_SHADER);


		this.mvpMatrix = new Matrix4f();
		this.viewMatrix = new Matrix4f();
		this.projectionMatrix = new Matrix4f();
		this.lightMatrix = new Matrix4f();


		this.root = new SceneObject();

		Plane plane = new Plane(this.simpleShader, 10);
		plane.setParent(this.root);
		plane.localMatrix.scale(5, 5, 5);

		Axes axes = new Axes(this.colourShader);
		axes.setParent(this.root);

		JSONObject jsonMap = this.level.getJSONObject("map");
		int width = jsonMap.getInt("width");
		int depth = jsonMap.getInt("depth");
		JSONArray heights = jsonMap.getJSONArray("height");

		grassTexture = loadTexture(GRASS_TEXTURE);
		map = new HeightMap(mapShader, width, depth, heights, grassTexture);
		map.setParent(this.root);
		map.localMatrix.scale(5, 1, 5);

		float waterHeight = this.level.getFloat("waterHeight");
		water = new Water(waterShader, width, depth, waterHeight / 5);
		water.setParent(this.root);
		water.localMatrix.scale(5, 5, 5);

		this.cameraPivot = new SceneObject();
		this.cameraPivot.setParent(this.root);

		this.camera = new SceneObject();
		this.camera.setParent(this.cameraPivot);
		this.camera.localMatrix.translate(0, cameraHeight, cameraDistance);

		this.lightPivot = new SceneObject();
		this.lightPivot.setParent(this.root);

		this.light = new Light(simpleShader, new float[] { 1.0f, 1.0f, 0.0f, 1.0f });
		this.light.setParent(this.lightPivot);
		this.light.localMatrix.translate(0, 0, lightDistance);
		this.light.localMatrix.scale(0.2f, 0.2f, 0.2f);

		this.line = new Line(simpleShader, lightDir);
		line.setParent(root);
	}


	private Shader loadShader(String vs, String fs) {
		try {
			File vertexShader = new File(SHADER_DIRECTORY, vs);
			File fragmentShader = new File(SHADER_DIRECTORY, fs);
			return new Shader(vertexShader, fragmentShader);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (GLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private int loadTexture(String textureFile) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		int textureID = 0;
		try {
			Texture tex = TextureIO.newTexture(new File(TEXTURE_DIRECTORY, textureFile), true);
			textureID = tex.getTextureObject();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

		return textureID;
	}

	private final float CAMERA_TURN = TAU / 8;
	private final float CAMERA_ZOOM = 1;
	private float cameraYaw = 0;
	private float cameraPitch = 0;
	private float cameraDistance = 10;
	private float cameraHeight = 1;
	private float moveForward = 0;

	private float cameraAspect = (float) screenWidth / screenHeight;
	private float cameraNear = 0.1f;
	private float cameraFar = 120.0f;

	float cameraFOV = TAU / 8;

	private Vector4f viewDir = new Vector4f();

	private float lightDistance = 5;
	private float lightYaw = -TAU / 4;
	private float lightPitch = -TAU / 8;
	private Vector4f lightDir = new Vector4f();

	public void update(float dt) {


		if (this.input.isKeyDown(KeyEvent.VK_UP)) {
			this.cameraPitch -= CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_DOWN)) {
			this.cameraPitch += CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_LEFT)) {
			this.cameraYaw -= CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_RIGHT)) {
			this.cameraYaw += CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_J)) {
			this.cameraFOV -= CAMERA_ZOOM * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_K)) {
			this.cameraFOV += CAMERA_ZOOM * dt;
		}

		this.cameraPivot.localMatrix.identity();

		if (this.input.isKeyDown(KeyEvent.VK_SPACE)) {
			moveForward = 0f;
			cameraFOV = TAU / 8;
		}
		Vector4f moveDir = viewDir.normalize(new Vector4f());
		moveDir.mul(moveForward);
		this.cameraPivot.localMatrix.translate(moveDir.x, moveDir.y, moveDir.z);
		this.cameraPivot.localMatrix.rotateY(cameraYaw);
		this.cameraPivot.localMatrix.rotateX(cameraPitch);

		
		this.viewDir.set(0, 0, -1, 0);
		this.camera.getWorldMatrix(this.viewMatrix);
		this.viewDir.mul(this.viewMatrix);

		if (this.input.isKeyDown(KeyEvent.VK_W)) {
			this.lightPitch -= CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_S)) {
			this.lightPitch += CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_A)) {
			this.lightYaw -= CAMERA_TURN * dt;
		}

		if (this.input.isKeyDown(KeyEvent.VK_D)) {
			this.lightYaw += CAMERA_TURN * dt;
		}

		this.lightPivot.localMatrix.identity();
		this.lightPivot.localMatrix.rotateY(lightYaw);
		this.lightPivot.localMatrix.rotateX(lightPitch);


		this.lightDir.set(0, 0, 1, 0);
		this.light.getWorldMatrix(this.lightMatrix);
		this.lightDir.mul(this.lightMatrix);
		this.lightDir.mul(lightDistance / 0.2f);

		line.setLightDir(lightDir);

		map.setLightDir(lightDir);
		map.setViewDir(viewDir);

		water.setLightDir(lightDir);
		water.setViewDir(viewDir);

		input.clear();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		long time = System.currentTimeMillis();
		float dt = (time - oldTime) / 1000.0f;
		oldTime = time;
		update(dt);

		gl.glViewport(0, 0, screenWidth, screenHeight);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		gl.glClearDepth(1f);
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

		this.camera.getWorldMatrix(this.viewMatrix);
		this.viewMatrix.invert();
		this.projectionMatrix.setPerspective(cameraFOV, cameraAspect, cameraNear, cameraFar);

		this.mvpMatrix.identity();
		this.mvpMatrix.mul(this.projectionMatrix);
		this.mvpMatrix.mul(this.viewMatrix);

		this.root.draw(mvpMatrix);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	/**
	 * Main method expects a JSON level filename to be give as an argument.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws GLException
	 */
	public static void main(String[] args) throws IOException, GLException {
		File levelFile = new File(args[0]);
		System.out.println(levelFile.getAbsolutePath());
		BufferedReader in = new BufferedReader(new FileReader(levelFile));
		JSONTokener tokener = new JSONTokener(in);
		JSONObject level = new JSONObject(tokener);

		new Assignment3(level);
	}

}
