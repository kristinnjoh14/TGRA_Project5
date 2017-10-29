package com.ru.tgra.game;


import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.ru.tgra.graphics.*;
import com.ru.tgra.graphics.shapes.*;
import com.ru.tgra.graphics.shapes.g3djmodel.G3DJModelLoader;
import com.ru.tgra.graphics.shapes.g3djmodel.MeshModel;

public class LabMeshTexGame extends ApplicationAdapter implements InputProcessor {

	Shader shader;

	private Camera cam;
	//private Camera topCam;
	private Point3D carPos;			//The position of the car
	private Vector3D carVelocity;	//The velocity of the car
	private Vector3D carOrientation;//The orientation of the car
	private float acceleration[];	//The acceleration of gears 1-5, reverse and braking, gears 1-5 are approximated, but realistic
	private int topSpeed[];			//The estimated top speed of each gear, scaled to fit world units
	private float diffRatio;		//A scalar by which to multiply acceleration, much like how gear ratios and drive ratios work in real life
	private float shiftTime;		//A constant up to which to count when shifting gears
	private float shift;			//A counter used to stop acceleration while shifting gears
	private boolean shifting;		//A boolean that is on when shifting gears and off otherwise
	private int previousGear;		//Index of previous gear used to figure out whether or not to do a shift timeout
	private float normalfov = 90.0f;//Initialization value for camera fov
	private float fov = normalfov;		//Camera field of view

	//AE86(https://sketchfab.com/models/0cab0e8b7fe647e9a1e0b434a6da56f1) 
	//by Victor Faria(https://sketchfab.com/IamBiscoito) 
	//is licensed under CC Attribution(http://creativecommons.org/licenses/by/4.0/)
	MeshModel corolla;
	Texture road;
	Texture skyBox;
	float[] roadUV = {
			0,0,
			0,0,
			0,0,
			0,0,
			
			0,0,
			0,0,
			0,0,
			0,0,
			
			0,0,
			0,0,
			0,0,
			0,0,
			
			0,0,
			1,0,
			1,1,
			0,1,
			
			0,0,
			0,0,
			0,0,
			0,0,
			
			0,0,
			0,0,
			0,0,
			0,0,
	};
	Random rand = new Random();

	@Override
	public void create () {
		carPos = new Point3D(0,0,0);
		carVelocity = new Vector3D(0,0,0);
		carOrientation = new Vector3D(0,0,1);
		
		acceleration = new float[] {
			3.6f,2.1f,1.4f,1,0.86f,-4,-25
		};
		topSpeed = new int[] {
			20,34,51,72,84,19,14,14,14
		};
		diffRatio = 2.5f;
		
		shiftTime = 0.35f;
		shifting = true;
		previousGear = -1;
		
		Gdx.input.setInputProcessor(this);

		DisplayMode disp = Gdx.graphics.getDesktopDisplayMode();
		Gdx.graphics.setDisplayMode(disp.width, disp.height, false);

		shader = new Shader();
		
		skyBox = new Texture(Gdx.files.internal("textures/cloudySeaBinary.jpg"));
		road = new Texture(Gdx.files.internal("textures/road.jpg"));

		corolla = G3DJModelLoader.loadG3DJFromFile("AE86smooth.g3dj");

		BoxGraphic.create();
		SphereGraphic.create();

		ModelMatrix.main = new ModelMatrix();
		ModelMatrix.main.loadIdentityMatrix();
		shader.setModelMatrix(ModelMatrix.main.getMatrix());

		cam = new Camera();
		cam.look(new Point3D(3f, 4f, -3f), new Point3D(0,4,0), new Vector3D(0,1,0));

		//topCam = new Camera();
		//orthoCam.orthographicProjection(-5, 5, -5, 5, 3.0f, 100);
		//topCam.perspectiveProjection(30.0f, 1, 3, 100);

		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	}
	
	private void accelerate(Boolean gas, int gear) {
		if(gas) {
			if(carOrientation.dot(carVelocity) < 0) {	//If braking from reversing
				//Scale change to velocity by braking force, stored after reverse gear gear
				carOrientation.scale(acceleration[6]*Gdx.graphics.getDeltaTime());
				carOrientation.scale(-1);
				carVelocity.add(carOrientation);
				carOrientation.normalize();
				if(carVelocity.length() < 3) {
					carVelocity.scale(0);
				}
			} else {	//If accelerating
				//Accelerate given the current gear in the direction the car is pointed in
				if(fov < normalfov + 40) {
					fov += Gdx.graphics.getDeltaTime()*5;
				}
				carOrientation.scale(acceleration[gear]*Gdx.graphics.getDeltaTime()*diffRatio);
				carVelocity.add(carOrientation);
				if(carVelocity.length() > topSpeed[4]) {
					carVelocity.normalize();
					carVelocity.scale(topSpeed[4]);
				}
				//Set length of the orientation vector to 1 again
				carOrientation.normalize();
			}
		} else {
			if(carOrientation.dot(carVelocity) > 0) {	//If braking
				//Scale change to velocity by braking force, stored after reverse gear gear
				if(fov > normalfov) {
					fov -= Gdx.graphics.getDeltaTime()*5;
				}
				carOrientation.scale(acceleration[6]*Gdx.graphics.getDeltaTime());
				carVelocity.add(carOrientation);
				carOrientation.scale(-1);
				carOrientation.normalize();
				if(carVelocity.length() < 3) {
					carVelocity.scale(0);
				}
			} else {	//If reversing
				//Scale change to velocity by reverse gear, stored after 5th gear
				carOrientation.scale(acceleration[5]*Gdx.graphics.getDeltaTime()*diffRatio);
				carVelocity.add(carOrientation);
				carOrientation.scale(-1);
				carOrientation.normalize();
				if(carVelocity.length() > topSpeed[5]) {
					carVelocity.normalize();
					carVelocity.scale(topSpeed[5]);
				}
			}
		}
	}
	private void input(float deltaTime)
	{
		if(Gdx.input.isKeyPressed(Input.Keys.A)) {
			cam.slide(-3.0f * deltaTime, 0, 0);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.D)) {
			cam.slide(3.0f * deltaTime, 0, 0);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.W)) {
			if(!shifting) {
				//Determine current gear index
				int gear = 0;
				for(int i = 4; i > -1; i--) {
					if(carVelocity.length() < topSpeed[i]) {
						gear = i;
					}
				}
				if(gear != previousGear) {
					System.out.println(carVelocity.length() + " " + gear);
					shifting = true;
				}
				accelerate(true, gear);
				previousGear = gear;
			}
		}
		if(Gdx.input.isKeyPressed(Input.Keys.S)) {
			accelerate(false, 5);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.R)) {
			cam.slide(0, 3.0f * deltaTime, 0);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.F)) {
			cam.slide(0, -3.0f * deltaTime, 0);
		}

		if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			cam.yaw(90.0f * deltaTime);
			//cam.rotateY(90.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			cam.yaw(-90.0f * deltaTime);
			//cam.rotateY(-90.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
			Vector3D down = new Vector3D(0,-1,0);
			if(down.dot(cam.n) <= 0.99) {
				cam.pitch(90.0f * deltaTime);
			}
		}
		if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			Vector3D up = new Vector3D(0,1,0);
			if(up.dot(cam.n) <= 0.99) {
				cam.pitch(-90.0f * deltaTime);
			}
		}
		/*if(Gdx.input.isKeyPressed(Input.Keys.Q)) {
			cam.roll(-90.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.E)) {
			cam.roll(90.0f * deltaTime);
		}*/

		if(Gdx.input.isKeyPressed(Input.Keys.T)) {
			fov -= 30.0f * deltaTime;
		}
		if(Gdx.input.isKeyPressed(Input.Keys.G)) {
			fov += 30.0f * deltaTime;
		}

		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			Gdx.graphics.setDisplayMode(500, 500, false);
			Gdx.app.exit();
		}
	}
	
	private void update()
	{
		float deltaTime = Gdx.graphics.getDeltaTime();
		input(deltaTime);

		//do all updates to the game
		Vector3D tmp = new Vector3D(carVelocity.x*deltaTime, carVelocity.y*deltaTime, carVelocity.z*deltaTime);
		carPos.add(tmp);
		if(shifting) {
			shift += deltaTime;
			System.out.println(shift);
			if(fov > normalfov) {
				fov -= deltaTime*12;
			}
			if(shift >= shiftTime) {
				shifting = false;
				shift = 0;
			}
		}
	}
	private void display()
	{
		//do all actual drawing and rendering here
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		//Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
/*
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		//Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
		//Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
*/
	
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		cam.perspectiveProjection(fov, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 400.0f);
		cam.look(new Point3D(carPos.x, carPos.y + 2, carPos.z - 5), carPos, new Vector3D(0,1,0));
		shader.setViewMatrix(cam.getViewMatrix());
		shader.setProjectionMatrix(cam.getProjectionMatrix());
		shader.setEyePosition(cam.eye.x, cam.eye.y, cam.eye.z, 1.0f);

		ModelMatrix.main.loadIdentityMatrix();

		shader.setLightPosition(cam.eye.x, cam.eye.y, cam.eye.z, 1.0f);

		shader.setSpotDirection(-cam.n.x, -cam.n.y, -cam.n.z, 0.0f);
		shader.setSpotExponent(3f);
		shader.setConstantAttenuation(0.2f);
		shader.setLinearAttenuation(0.01f);
		shader.setQuadraticAttenuation(0.1f);

		shader.setLightColor(0.8f, 0.7f, 0.65f, 1.0f);
		
		shader.setGlobalAmbient(0.3f, 0.3f, 0.3f, 1);

		shader.setMaterialDiffuse(1.0f, 1.0f, 1.0f, 1.0f);
		shader.setMaterialSpecular(1.0f, 1.0f, 1.0f, 1.0f);
		shader.setMaterialEmission(0, 0, 0, 1);
		shader.setShininess(50.0f);

		//Draw car
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(carPos.x, carPos.y, carPos.z);
		ModelMatrix.main.addScale(0.5f, 0.5f, 0.5f);
		shader.setModelMatrix(ModelMatrix.main.getMatrix());
		corolla.draw(shader);

		ModelMatrix.main.popMatrix();
		
		//Draw skybox
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(cam.eye.x, cam.eye.y, cam.eye.z);
		ModelMatrix.main.addScale(150, 150, 150);
		shader.setModelMatrix(ModelMatrix.main.getMatrix());
		BoxGraphic.drawSolidCube(shader, skyBox);
		
		ModelMatrix.main.popMatrix();
		
		//Draw road
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(0, -2, 800);
		ModelMatrix.main.addScale(1600, 1, 1600);
		shader.setModelMatrix(ModelMatrix.main.getMatrix());
		BoxGraphic.setUVArray(roadUV);
		BoxGraphic.drawSolidCube(shader, road);
		BoxGraphic.defaultUVArray();
		ModelMatrix.main.popMatrix();
	}
	@Override
	public void render () {
		//put the code inside the update and display methods, depending on the nature of the code
		update();
		display();

	}
	

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}


}