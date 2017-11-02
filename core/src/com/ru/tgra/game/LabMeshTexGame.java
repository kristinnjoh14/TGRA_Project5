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
	//private Camera topCam;		//Hypothetical future roadmap
	
	private Point3D carPos;			//The position of the car
	private Vector3D carSpeed;	//The velocity of the car
	private Vector3D carOrientation;//The orientation of the car
	private float acceleration[];	//The acceleration of gears 1-5, reverse and braking, gears 1-5 are approximated, but realistic
	private int topSpeed[];			//The estimated top speed of each gear, scaled to fit world units
	private float diffRatio;		//A scalar by which to multiply acceleration, much like how gear ratios and drive ratios work in real life
	private float shiftTime;		//A constant up to which to count when shifting gears
	private float shift;			//A counter used to stop acceleration while shifting gears
	private boolean shifting;		//A boolean that is on when shifting gears and off otherwise
	private int previousGear;		//Index of previous gear used to figure out whether or not to do a shift timeout
	private float boostGain;		//A proportion by which to multiply friction speed loss to add to boost
	private float boostPower;		//The power of boost when applied
	private float normalfov = 90.0f;//Initialization value for camera fov
	private float fov = normalfov;		//Camera field of view
	private float maximumSteeringAngle;	//The angle by which to steer the car. Could be a maximum if input weren't binary
	private float minDriftSpeed;	//Minimum driving speed to initiate a drift
	private boolean drifting;		//A boolean that is on while drifting and off otherwise
	private boolean gripping;	//A boolean that is on while the user intends to drift. This increases maximum steering angle, despite a lack of "grip"
	private float accumulatedDriftBoost;	//A counter that adds up all the speed you've lost to friction, a fraction of
										//which multiplied by a scalar will be re-applied to the car as a boost
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
		carSpeed = new Vector3D(0,0,0);
		carOrientation = new Vector3D(0,0,1);
		maximumSteeringAngle = 100;
		
		acceleration = new float[] {
			3.6f,2.1f,1.4f,1,0.86f,-4,-25,-20
		};
		topSpeed = new int[] {
			20,34,51,72,84,19,14,14,14
		};
		diffRatio = 2.5f;
		
		shiftTime = 0.35f;
		shifting = true;
		previousGear = -1;
		
		minDriftSpeed = topSpeed[0];
		drifting = false;
		
		boostGain = 1;
		boostPower = 30f;
		
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
	private void maybeBoost(float deltaTime) {
		if(accumulatedDriftBoost < 1) {
			accumulatedDriftBoost = 0;
		} else {
			boost(deltaTime);
		}
	}
	private void moveCar(float deltaTime) {
		Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
		tmp.scale(deltaTime);
		carPos.add(tmp);
	}
	private void shift(float deltaTime) {	//timeout until deltaTime accumulates 0.35 (350 milliseconds)
		if(shifting) {
			shift += deltaTime;
			if(fov > normalfov) {
				fov -= deltaTime*12;
			}
			if(shift >= shiftTime) {
				shifting = false;
				shift = 0;
			}
		}
	}
	private void updateFov(float deltaTime) {
		if(targetFovBySpeed() - fov < 0) {
			if(targetFovBySpeed() - fov < -2) {
				fov -= deltaTime*3;
			}
		} else {
			if(targetFovBySpeed() - fov > 2) {
				fov += deltaTime*3;
			}
		}
	}
 	private float targetSteeringAngleBySpeed() {
		//TODO: return steering angle between maximumSteeringAngle and some minimum depending on speed
		return maximumSteeringAngle;
	}
	private float targetFovBySpeed() {
		return normalfov + carSpeed.length()/topSpeed[4]*40;
	}
	private void accelerate(Boolean gas, int gear) {
		if(gas) {
			if(carOrientation.dot(carSpeed) < 0) {	//If braking from reversing
				//Scale change to velocity by braking force, stored after reverse gear gear
				Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
				tmp.normalize();
				tmp.scale(acceleration[6]*Gdx.graphics.getDeltaTime());
				carSpeed.add(tmp);
				if(carSpeed.length() < 5) {
					carSpeed.scale(0);
					shifting = true;
				}
			} else {	//If accelerating
				//Accelerate given the current gear in the direction the car is pointed in
				//Change fov during acceleration
				if(fov < normalfov + 40) {
					fov += Gdx.graphics.getDeltaTime()*acceleration[gear]*3;
				}
				carOrientation.scale(acceleration[gear]*Gdx.graphics.getDeltaTime()*diffRatio);
				carSpeed.add(carOrientation);
				if(carSpeed.length() > topSpeed[4]) {	//Do not exceed top speed
					carSpeed.normalize();
					carSpeed.scale(topSpeed[4]);
				}
				//Set length of the orientation vector to 1 again
				carOrientation.normalize();
			}
		} else {
			if(carOrientation.dot(carSpeed) > 0) {	//If braking
				if(fov > normalfov) {
					fov += Gdx.graphics.getDeltaTime()*acceleration[6]*3;
				}
				//Scale change to velocity by braking force, stored after reverse gear gear
				Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
				tmp.normalize();
				tmp.scale(acceleration[6]*Gdx.graphics.getDeltaTime());
				carSpeed.add(tmp);
				if(carSpeed.length() < 5) {
					carSpeed.scale(0);
					shifting = true;
					System.out.println("stopped");
				}
			} else {	//If reversing
				//Scale change to velocity by reverse gear, stored after 5th gear
				carOrientation.scale(acceleration[5]*Gdx.graphics.getDeltaTime()*diffRatio);
				carSpeed.add(carOrientation);
				carOrientation.scale(-1);
				carOrientation.normalize();
				if(carSpeed.length() > topSpeed[5]) {
					carSpeed.normalize();
					carSpeed.scale(topSpeed[5]);
				}
			}
		}
	}
	private void boost(float deltaTime) {
		carOrientation.scale(boostPower*deltaTime);
		accumulatedDriftBoost -= carOrientation.length();
		if(fov < normalfov+40) {
			fov += carOrientation.length();
		}
		carSpeed.add(carOrientation);
		carOrientation.normalize();
		//System.out.println("Boost" + accumulatedDriftLoss);
	}
	private void turn(float steeringAngle) {
		if(carSpeed.length() < 5) {
			steeringAngle = (steeringAngle/Math.abs(steeringAngle))*carSpeed.length()/(maximumSteeringAngle/20);
		}
		if(carSpeed.length() == 0)
			steeringAngle = 0;
		if(gripping & !drifting) {
			//TODO:lower steering angle the faster you drive
		}
		turnVector(carOrientation, steeringAngle);
		if(!drifting) {
			if(carSpeed.length() > minDriftSpeed & !gripping) {
				steeringAngle *= 0.55f;	//TODO: Maybe make this dependant on speed
			}
			turnVector(carSpeed, steeringAngle);
		}
	}
	private void turnVector(Vector3D v, float angle) {
		float radians = angle * (float)Math.PI / 180.0f;
		float cos = (float)Math.cos(radians);
		float sin = -(float)Math.sin(radians);
		float x = v.x;
		float z = v.z;
		v.x = cos*x + sin*z;
		v.z = -sin*x + cos*z;
	}
	private void input(float deltaTime)
	{
		if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
			gripping = false;
		}
		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
			maybeBoost(deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.A)) {
			turn(-targetSteeringAngleBySpeed()*deltaTime);	//TODO: Fix reversing steering direction
		}
		if(Gdx.input.isKeyPressed(Input.Keys.D)) {
			turn(targetSteeringAngleBySpeed()*deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.W)) {
			if(!shifting) {
				int gear = 0;			//Determine current gear index
				for(int i = 4; i > -1; i--) {
					if(carSpeed.length() < topSpeed[i]) {
						gear = i;
					}
				}
				if(gear != previousGear) {
					shifting = true;
				}
				accelerate(true, gear);
				previousGear = gear;
			}
		}
		if(Gdx.input.isKeyPressed(Input.Keys.S)) {
			if(!shifting) {
				accelerate(false, 5);
			}
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			Gdx.graphics.setDisplayMode(500, 500, false);
			Gdx.app.exit();
		}
		/*if(Gdx.input.isKeyPressed(Input.Keys.A)) {
			cam.slide(-3.0f * deltaTime, 0, 0);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.D)) {
			cam.slide(3.0f * deltaTime, 0, 0);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.W)) {
			cam.slide(0, 0, -3.0f * deltaTime);
			//cam.walkForward(3.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.S)) {
			cam.slide(0, 0, 3.0f * deltaTime);
			//cam.walkForward(-3.0f * deltaTime);
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
			cam.pitch(90.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			cam.pitch(-90.0f * deltaTime);
		}

		if(Gdx.input.isKeyPressed(Input.Keys.Q)) {
			cam.roll(-90.0f * deltaTime);
		}
		if(Gdx.input.isKeyPressed(Input.Keys.E)) {
			cam.roll(90.0f * deltaTime);
		}

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
		}*/
	}
	private void drift(float deltaTime) { 
		Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
		float dot = 0;
		float len = tmp.length();
		if(len > 0) {
			tmp.normalize();
			dot = tmp.dot(carOrientation);
			if(dot < 0.9 & dot > -0.5) {
				drifting = true;
				tmp.scale(acceleration[7]*deltaTime);	//Scale deceleration by drift-grip, stored after braking force
				if(carSpeed.length() < 1) {
					carSpeed.set(0,0,0);
				} else {
					carSpeed.add(tmp);
				}
				Vector3D cross = carSpeed.cross(carOrientation);	//Cross product used to determine if right or left drift
				float ang = targetSteeringAngleBySpeed();
				if(cross.dot(new Vector3D(0,1,0)) > 0) {
					ang = -ang;
				}
				turnVector(carSpeed, dot*ang*deltaTime);	//carSpeed rotated toward carOrientation by an amount proportional to drift angle
				accumulatedDriftBoost += tmp.length()*boostGain;
			} else {
				if(gripping) {
					carSpeed.set(carOrientation.x, carOrientation.y, carOrientation.z);
					carSpeed.scale(len);
					carSpeed.scale(dot);
				}
				drifting = false;
			}
		}
	}
	
	private void update()
	{
		float deltaTime = Gdx.graphics.getDeltaTime();
		input(deltaTime);
		//TODO: Turn carVelocity while drifting. Also, refactor once there is no need for the debug print lines
		drift(deltaTime);		//Determine if the car is in a drift and perform maths to apply to it's speed if it is
		moveCar(deltaTime);		//Move car along carVelocity
		shift(deltaTime);		//Timeout if changing gears
		updateFov(deltaTime);	//Slowly set camera to the fov of the current speed
		gripping = true;		//Reset the grip bool for the next frame
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
		
		cam.perspectiveProjection(fov, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 700.0f);
		cam.look(new Point3D(carPos.x-carOrientation.x*5, carPos.y+2, carPos.z-carOrientation.z*5), carPos, new Vector3D(0,1,0));
		shader.setViewMatrix(cam.getViewMatrix());
		shader.setProjectionMatrix(cam.getProjectionMatrix());
		shader.setEyePosition(cam.eye.x, cam.eye.y, cam.eye.z, 1.0f);

		ModelMatrix.main.loadIdentityMatrix();

		shader.setLightPosition(cam.eye.x,cam.eye.y,cam.eye.z, 1.0f);

		shader.setSpotDirection(-cam.n.x, -cam.n.y, -cam.n.z, 0.0f);
		shader.setSpotExponent(3f);
		shader.setConstantAttenuation(0.2f);
		shader.setLinearAttenuation(0.0f);
		shader.setQuadraticAttenuation(0.1f);

		shader.setLightColor(0.8f, 0.7f, 0.65f, 1.0f);
		
		shader.setGlobalAmbient(0.3f, 0.3f, 0.3f, 1);

		shader.setMaterialDiffuse(1.0f, 1.0f, 1.0f, 1.0f);
		shader.setMaterialSpecular(1.0f, 1.0f, 1.0f, 1.0f);
		shader.setMaterialEmission(0, 0, 0, 1);
		shader.setShininess(50.0f);
		
		//Draw skybox
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(cam.eye.x, cam.eye.y, cam.eye.z);
		ModelMatrix.main.addScale(800, 800, 800);
		shader.setModelMatrix(ModelMatrix.main.getMatrix());
		BoxGraphic.drawSolidCube(shader, skyBox);
		ModelMatrix.main.popMatrix();

		//Draw car
		float angle = (float)((180/Math.PI)*Math.acos(carOrientation.dot(new Vector3D(0,0,1))));
		if(carOrientation.x < 0)
			angle = -angle;
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(carPos.x, carPos.y, carPos.z);
		ModelMatrix.main.addScale(0.5f, 0.5f, 0.5f);
		ModelMatrix.main.addRotationY(angle);
		shader.setModelMatrix(ModelMatrix.main.getMatrix());
		corolla.draw(shader);

		ModelMatrix.main.popMatrix();
		
		//Draw road
		ModelMatrix.main.pushMatrix();
		ModelMatrix.main.addTranslation(0, -2, 0);
		ModelMatrix.main.addScale(50, 1, 50);
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