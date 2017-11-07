package com.ru.tgra.game;


import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.ru.tgra.graphics.*;
import com.ru.tgra.graphics.shapes.*;
import com.ru.tgra.graphics.shapes.g3djmodel.G3DJModelLoader;
import com.ru.tgra.graphics.shapes.g3djmodel.MeshModel;

public class LabMeshTexGame extends ApplicationAdapter implements InputProcessor {

	Shader shader;

	private Camera cam;
	//private Camera topCam;			//Hypothetical future roadmap
	private Camera camera;
	private Point3D carPos;				//The position of the car
	private Vector3D carSpeed;			//The velocity of the car
	private Vector3D carOrientation;	//The orientation of the car
	private boolean chaseCam;			//A toggle-able camera position state
	private float acceleration[];		//The acceleration of gears 1-5, reverse and braking, gears 1-5 are approximated, but realistic
	private int topSpeed[];				//The estimated top speed of each gear, scaled to fit world units
	private float diffRatio;			//A scalar by which to multiply acceleration, much like how gear ratios and drive ratios work in real life
	private float shiftTime;			//A constant up to which to count when shifting gears
	private float shift;				//A counter used to stop acceleration while shifting gears
	private boolean shifting;			//A boolean that is on when shifting gears and off otherwise
	private int previousGear;			//Index of previous gear used to figure out whether or not to do a shift timeout
	private float boostGain;			//A proportion by which to multiply friction speed loss to add to boost
	private float boostPower;			//The power of boost when applied
	private float maxBoost;				//The car's maximum nitrous capacity
	private float normalfov = 90.0f;	//Initialization value for camera fov
	private float fov = normalfov;		//Camera field of view
	private float maximumSteeringAngle;	//The angle by which to steer the car. Could be a maximum if input weren't binary
	private float minDriftSpeed;		//Minimum driving speed to initiate a drift
	private boolean drifting;			//A boolean that is on while drifting and off otherwise
	private boolean gripping;	//A boolean that is on while the user intends to drift. This increases maximum steering angle, despite a lack of "grip"
	private float accumulatedDriftBoost;//A counter that adds up all the speed you've lost to friction, a fraction of which will accumulate as boost
	private Sound sound; 		//Ingame music
	private SpriteBatch batch;

	MeshModel corolla;		//AE86(https://sketchfab.com/models/0cab0e8b7fe647e9a1e0b434a6da56f1) 
							//by Victor Faria(https://sketchfab.com/IamBiscoito) 
							//is licensed under CC Attribution(http://creativecommons.org/licenses/by/4.0/)
	MeshModel hayai;		//Speedometer without markings
	
	Texture dash;
	Texture road;
	Texture skyBox;
	Texture arrow;
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
	float[] speedUV = {
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
		
		boostGain = 0.6f;
		boostPower = 30f;
		maxBoost = 60;
		
		Gdx.input.setInputProcessor(this);

		DisplayMode disp = Gdx.graphics.getDesktopDisplayMode();
		Gdx.graphics.setDisplayMode(disp.width, disp.height, false);

		shader = new Shader();
		
		skyBox = new Texture(Gdx.files.internal("textures/cloudySeaBinary.jpg"));
		road = new Texture(Gdx.files.internal("textures/road.jpg"));
		arrow = new Texture(Gdx.files.internal("textures/arrow.png"));
		dash = new Texture(Gdx.files.internal("textures/dash.png"));

		corolla = G3DJModelLoader.loadG3DJFromFile("AE86smooth.g3dj");
		hayai = G3DJModelLoader.loadG3DJFromFile("needle.g3dj");
		
		sound = Gdx.audio.newSound(Gdx.files.internal("sounds/90.wav"));
		//sound.play();
		//sound.loop();
		BoxGraphic.create();
		SphereGraphic.create();

		ModelMatrix.main = new ModelMatrix();
		ModelMatrix.main.loadIdentityMatrix();
		shader.setModelMatrix(ModelMatrix.main.getMatrix());

		cam = new Camera();
		cam.look(new Point3D(3f, 4f, -3f), new Point3D(0,4,0), new Vector3D(0,1,0));
		
		camera = new Camera();
		camera.perspectiveProjection(normalfov, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 100.0f);

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
		Vector3D tmp;
		if(carPos.z <= -13)
		{
			carPos.z = -8;
			carSpeed.z = 0;
			carSpeed.x = 0;
		}
		if(carPos.z >= 2983)
		{
			carPos.z = 2978f;
			carSpeed.z = 0;
			carSpeed.x = 0;
		}
		if(carPos.x <= -13)
		{
			carPos.x = -8;
			carSpeed.x = 0;
			carSpeed.z = 0;
		}
		if(carPos.x >= 43)
		{
			carPos.x = 38;
			carSpeed.x = 0;
			carSpeed.z = 0;
		}
		tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
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
		return normalfov + (carSpeed.length()/topSpeed[4])*40;
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
				carOrientation.scale(acceleration[gear]*Gdx.graphics.getDeltaTime()*diffRatio);
				carSpeed.add(carOrientation);
				if(carSpeed.length() > topSpeed[4]) {	//Do not exceed top speed
					carSpeed.normalize();
					carSpeed.scale(topSpeed[4]);
				} else {
					if(fov < normalfov + 40) {
						fov += Gdx.graphics.getDeltaTime()*acceleration[gear]*3;
					}
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
				steeringAngle *= 0.55f;	//TODO: Maybe make this dependent on speed
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
					} else if(carSpeed.length() >= topSpeed[4]) {
						gear = 4;
					}
				}
				if(gear != previousGear) {
					System.out.println(carSpeed.length());
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
		if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
			if(chaseCam == false)
			{
				chaseCam = true;
			}else{
				chaseCam = false;
			}
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			Gdx.graphics.setDisplayMode(500, 500, false);
			Gdx.app.exit();
		}
	}
	private void drift(float deltaTime) { 
		Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
		float dot = 0;
		float len = tmp.length();
		if(len > 0) {
			tmp.normalize();
			dot = tmp.dot(carOrientation);
			if(dot < 0.95 & dot > -0.5) {
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
				accumulatedDriftBoost = Math.min(maxBoost, accumulatedDriftBoost);
			} else {
				if(gripping) {
					if(dot < 0.999 & dot > -0.5) {
						Vector3D cross = carSpeed.cross(carOrientation);	//Cross product used to determine if right or left drift
						float ang = targetSteeringAngleBySpeed();
						if(cross.dot(new Vector3D(0,1,0)) > 0) {
							ang = -ang;
						}
						turnVector(carSpeed, dot*ang*deltaTime);
					} else {
						carSpeed.set(carOrientation.x, carOrientation.y, carOrientation.z);
						carSpeed.scale(len);
						carSpeed.scale(dot);
					}
				}
				drifting = false;
			}
		}
	}
	
	private void update()
	{
		float deltaTime = Gdx.graphics.getDeltaTime();
		input(deltaTime);
		drift(deltaTime);		//Determine if the car is in a drift and perform maths to apply to it's speed if it is
		moveCar(deltaTime);		//Move car along carVelocity
		shift(deltaTime);		//Timeout if changing gears
		updateFov(deltaTime);	//Slowly set camera to the fov of the current speed
		gripping = true;		//Reset the grip bool for the next frame
	}
	private void display()
	{
		for(int j = 0; j < 2; j++)
		{
			if(j == 0)
			{
				//do all actual drawing and rendering here
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
				Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

				Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				
				
				cam.perspectiveProjection(fov, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 700.0f);
				if(chaseCam && carSpeed.length() > 1) {
					Vector3D tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
					tmp.normalize();
					cam.look(new Point3D(carPos.x-tmp.x*6, carPos.y+3.5f, carPos.z-tmp.z*6), carPos, new Vector3D(0,1,0));
				} else {
					cam.look(new Point3D(carPos.x-carOrientation.x*6, carPos.y+3.5f, carPos.z-carOrientation.z*6), carPos, new Vector3D(0,1,0));
				}
				shader.setViewMatrix(cam.getViewMatrix());
				shader.setProjectionMatrix(cam.getProjectionMatrix());
				shader.setEyePosition(cam.eye, 1.0f);
		
				ModelMatrix.main.loadIdentityMatrix();
				
				shader.setLightPosition(cam.eye.x, cam.eye.y + 52, cam.eye.z, 1);
				shader.setLightColor(0, 0, 0, 0);	//Could use, but ambient + headlights looks pretty good, fits with the low sun and kind of feel
				shader.setHeadlightColor(0.8f, 0.7f, 0.65f, 1.0f);
				Vector3D headlightShift = carOrientation.cross(new Vector3D(0,1,0));
				headlightShift.scale(0.82f);
				shader.setLeftHeadlightPosition(carPos.x + carOrientation.x*2.1f + headlightShift.x,carPos.y+carOrientation.y*2.1f + headlightShift.y,carPos.z+carOrientation.z*2.1f + headlightShift.z, 1.0f);
				shader.setRightHeadlightPosition(carPos.x + carOrientation.x*2.1f - headlightShift.x,carPos.y+carOrientation.y*2.1f - headlightShift.y,carPos.z+carOrientation.z*2.1f - headlightShift.z, 1.0f);
				shader.setHeadlightDirection(carOrientation.x, carOrientation.y, carOrientation.z, 0.0f);
				shader.setHeadlightExponent(5f);
				shader.setConstantAttenuation(0.05f);
				shader.setLinearAttenuation(0.005f);
				shader.setQuadraticAttenuation(0.0001f);
						
				shader.setGlobalAmbient(0.2f, 0.2f, 0.2f, 1);
		
				shader.setMaterialDiffuse(1.0f, 1.0f, 1.0f, 1.0f);
				shader.setMaterialSpecular(1.0f, 1.0f, 1.0f, 1.0f);
				shader.setMaterialAmbient(1, 1, 1, 1);
				shader.setMaterialShininess(50);
				
				//Draw skybox
				shader.setMaterialEmission(0.4f, 0.4f, 0.4f, 1f);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(cam.eye.x, cam.eye.y, cam.eye.z);
				ModelMatrix.main.addScale(800, 800, 800);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.drawSolidCube(shader, skyBox);
				ModelMatrix.main.popMatrix();
				shader.setMaterialEmission(0, 0, 0, 1);
		
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
			
				int size = 30;
		
				//Draw road
					for(int i = 0; i < 100; i++)
				{
					ModelMatrix.main.pushMatrix();
					ModelMatrix.main.addTranslation(0, -2, i*size-0.5f);
					ModelMatrix.main.addScale(size, 1, size);
					shader.setModelMatrix(ModelMatrix.main.getMatrix());
					BoxGraphic.setUVArray(roadUV);
					BoxGraphic.drawSolidCube(shader, road);
					BoxGraphic.defaultUVArray();
					ModelMatrix.main.popMatrix();
				}
				for(int i = 0; i < 100; i++)
				{
					ModelMatrix.main.pushMatrix();
					ModelMatrix.main.addTranslation(size, -2, i*size-0.5f);
					ModelMatrix.main.addScale(size, 1, size);
					shader.setModelMatrix(ModelMatrix.main.getMatrix());
					BoxGraphic.setUVArray(roadUV);
					BoxGraphic.drawSolidCube(shader, road);
					BoxGraphic.defaultUVArray();
					ModelMatrix.main.popMatrix();
				}
			} else {
				Gdx.gl.glViewport(4*Gdx.graphics.getWidth()/5, 0, Gdx.graphics.getWidth()/5, Gdx.graphics.getHeight()/4);
				camera.look(new Point3D(10,100000,9), new Point3D(10,100000,7), new Vector3D(0,10000,0));
				camera.perspectiveProjection(normalfov, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 100.0f);
				Rectangle scissors = new Rectangle(4*Gdx.graphics.getWidth()/5, 0, Gdx.graphics.getWidth()/5, Gdx.graphics.getHeight()/5);
				ScissorStack.pushScissors(scissors);
				//Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
				shader.setViewMatrix(camera.getViewMatrix());
				shader.setProjectionMatrix(camera.getProjectionMatrix());
				shader.setMaterialSpecular(0, 0, 0, 0);
				shader.setMaterialDiffuse(0, 0, 0, 0);
				shader.setMaterialAmbient(1, 1, 1, 1);
				shader.setMaterialEmission(0.8f, 0.8f, 0.8f, 1);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(11.5f,100000,7);
				ModelMatrix.main.addScale(0.2f, 0.2f, 0.2f);
				ModelMatrix.main.addRotationZ(120-carSpeed.length()*2.8f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				hayai.draw(shader);
				ModelMatrix.main.popMatrix();
				
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(8.5f,100000,7);
				ModelMatrix.main.addScale(0.2f, 0.2f, 0.2f);
				ModelMatrix.main.addRotationZ(120-accumulatedDriftBoost*4f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				hayai.draw(shader);
				ModelMatrix.main.popMatrix();
				
				ScissorStack.popScissors();
			}
		}
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