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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
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
	private float engineRPM;			//Doesn't really represent rpm, but is used where rpm would
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
	private float accumulatedDriftBoostAtStartOfLap;//To know how much boost the player started a lap with
	private Sound sound; 				//Ingame music
	private Sound engine; 				//Engine noise
	private long noise;					//Engine noise id	

	private int gear;
	private float time = 0;				//Counts the time from start to finish
	
	private Sound gasSong;      //Boost music
	private float[][] grid;     //Create map
	private int length;
	private int width;
	private int size;           //The size of the roads, recommend not going below 30
	private Timer currTime;     //Timer so the song plays for a limited time
	private Boolean gasPlaying; //Boolean so the song dosen't start again
	private Boolean boolStars;  //Boolean so there won't be song conflict

	
	MeshModel corolla;		//AE86(https://sketchfab.com/models/0cab0e8b7fe647e9a1e0b434a6da56f1) 
							//by Victor Faria(https://sketchfab.com/IamBiscoito) 
							//is licensed under CC Attribution(http://creativecommons.org/licenses/by/4.0/)
	MeshModel hayai;		//3D speedo/tach/whatever gauge needle
	
	Texture tach;
	Texture speedo;
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
		diffRatio = 3f;
		
		gear = 0;
		shiftTime = 0.45f;
		shifting = true;
		previousGear = -1;
		
		minDriftSpeed = topSpeed[0];
		drifting = false;
		
		accumulatedDriftBoostAtStartOfLap = 0;
		boostGain = 0.85f;
		boostPower = 30f;
		maxBoost = 60;
				
		Gdx.input.setInputProcessor(this);

		DisplayMode disp = Gdx.graphics.getDesktopDisplayMode();
		Gdx.graphics.setDisplayMode(disp.width, disp.height, false);

		shader = new Shader();
		
		skyBox = new Texture(Gdx.files.internal("textures/cloudySeaBinary.jpg"));
		road = new Texture(Gdx.files.internal("textures/road.jpg"));
		arrow = new Texture(Gdx.files.internal("textures/arrow.png"));
		speedo = new Texture(Gdx.files.internal("textures/speedo.png"));
		tach = new Texture(Gdx.files.internal("textures/tach.png"));
		
		corolla = G3DJModelLoader.loadG3DJFromFile("AE86smooth.g3dj");
		hayai = G3DJModelLoader.loadG3DJFromFile("needle.g3dj");
		
		sound = Gdx.audio.newSound(Gdx.files.internal("sounds/90.wav"));
		engine = Gdx.audio.newSound(Gdx.files.internal("sounds/engine.mp3"));

		sound.play();
		sound.loop();
		noise = engine.play(1.0f);
		engine.setLooping(noise, true);
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
		
		size = 30;
		 
		carPos = new Point3D(size/2,0,size/2);
		gasPlaying = false;
		gasSong = Gdx.audio.newSound(Gdx.files.internal("sounds/gas.mp3"));
		currTime = new Timer();
		boolStars = false;
		width = 30;
        length = 30;
        grid = new float[width][length];
              
        grid[0][0] = 1;
		grid[0][1] = 1;
		grid[0][2] = 1;
		grid[1][2] = 1;
		grid[1][3] = 1;
		grid[1][4] = 1;
		grid[0][4] = 1;
		grid[0][5] = 1;
		grid[0][6] = 1;
		grid[1][6] = 1;
		grid[2][6] = 1;
		grid[3][6] = 1;
		grid[4][6] = 1;
		grid[5][6] = 1;
		grid[6][6] = 1;
		grid[7][6] = 1;
		grid[8][6] = 1;
		grid[9][6] = 1;
		grid[9][5] = 1;
		grid[9][4] = 1;
		grid[8][4] = 1;
		grid[7][4] = 1;
		grid[6][4] = 1;
		grid[6][3] = 1;
		grid[5][3] = 1;
		grid[5][2] = 1;
		grid[5][1] = 1;
		grid[6][1] = 1;
		grid[7][1] = 1;
		grid[8][1] = 1;
		grid[9][1] = 1;
		grid[9][2] = 1;
		grid[10][2] = 1;
		grid[11][2] = 1;
		grid[12][2] = 1;
		grid[12][3] = 1;
		grid[12][4] = 1;
		grid[12][5] = 1;
		grid[13][5] = 1;
		grid[13][6] = 1;
		grid[13][7] = 1;
		grid[14][7] = 1;
		grid[15][7] = 1;
		grid[16][7] = 1;
		grid[16][8] = 1;
		grid[16][9] = 1;
		grid[15][9] = 1;
		grid[14][9] = 1;
		grid[14][10] = 1;
		grid[14][11] = 1;
		grid[14][12] = 1;
		grid[14][13] = 1;
		grid[15][13] = 1;
		grid[16][13] = 1;
		grid[16][12] = 1;
		grid[16][11] = 1;
		grid[17][11] = 1;
		grid[18][11] = 1;
		grid[19][11] = 1;
		grid[20][11] = 1;
		grid[20][12] = 1;
		grid[20][13] = 1;
		grid[20][14] = 1;
		grid[20][15] = 1;
		grid[20][16] = 1;
		grid[20][17] = 1;
		grid[20][18] = 1;
		grid[21][18] = 1;
		grid[22][18] = 1;
		grid[22][19] = 1;
		grid[22][20] = 1;
		grid[23][20] = 1;
		grid[23][21] = 1;
		grid[24][21] = 1;
		grid[25][21] = 1;
		grid[25][20] = 1;
		grid[25][19] = 1;
		grid[26][19] = 1;
		grid[27][19] = 1;
		grid[27][20] = 1;
		grid[27][21] = 1;
		grid[27][22] = 1;
		grid[27][23] = 1;
		grid[27][24] = 1;
		grid[27][25] = 1;
		grid[26][25] = 1;
		grid[25][25] = 1;
		grid[25][24] = 1;
		grid[24][24] = 1;
	}
	private void maybeBoost(float deltaTime) {
		if(accumulatedDriftBoost < 1) {
			accumulatedDriftBoost = 0;
		} else {
			boost(deltaTime);
			if(!gasPlaying)
			{
				gasPlaying = true;
				sound.pause();
				gasSong.play();
				currTime.scheduleTask(new Task() {

			        public void run() 
			        {
			        	gasPlaying = false;
						gasSong.stop();
						sound.resume();
						currTime.clear();
					}
			    }, 5.4f);
			}
		}
	}
	private void moveCar(float deltaTime) {
		Vector3D tmp;
		checkCollisions();
		tmp = new Vector3D(carSpeed.x, carSpeed.y, carSpeed.z);
		tmp.scale(deltaTime);
		
		carPos.add(tmp);
		if(!boolStars &&carPos.x >= 24*size && carPos.z >= 24*size && carPos.x <= 25*size && carPos.z <= 25*size)
		{
			boolStars = true;
			sound.stop();
			sound = Gdx.audio.newSound(Gdx.files.internal("sounds/stars.wav"));

			sound.play();
			sound.loop();
			skyBox = new Texture(Gdx.files.internal("textures/download.jpg"));
			shader.setLightColor(0.3f, 0.2f, 0.25f, 1);
			carOrientation.scale(-1);
			carSpeed.scale(0);
			System.out.println("Time(reverse track): " + time + ". Lap started with " + accumulatedDriftBoostAtStartOfLap + " pffts in the boost tank");
			accumulatedDriftBoostAtStartOfLap = accumulatedDriftBoost;
			time = 0;
		}
		if(boolStars &&carPos.x >= 0 && carPos.z >= 0 && carPos.x <= size && carPos.z <= size)
		{
			boolStars = false;
			sound.stop();
			sound = Gdx.audio.newSound(Gdx.files.internal("sounds/90.wav"));

			sound.play();
			sound.loop();
			skyBox = new Texture(Gdx.files.internal("textures/cloudySeaBinary.jpg"));
			shader.setLightColor(0.4f, 0.3f, 0.35f, 1);
			carOrientation.scale(-1);
			carSpeed.scale(0);
			System.out.println("Time: " + time + ". Lap started with " + accumulatedDriftBoostAtStartOfLap + " pffts in the boost tank");
			accumulatedDriftBoostAtStartOfLap = accumulatedDriftBoost;
			time = 0;
		}

		
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
				gear = 0;			//Determine current gear index
				for(int i = 4; i > -1; i--) {
					if(carSpeed.length() < topSpeed[i]) {
						gear = i;
					} else if(carSpeed.length() >= topSpeed[4]) {
						gear = 4;
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
			if(dot < 0.97 & dot > -0.5) {
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
	private void checkCollisions()

    {
        float x = carPos.x;
        float z = carPos.z;
        x -= x % size;
        z -= z % size;
        x = (int) Math.floor(x/size);
        z = (int) Math.floor(z/size);
       
        if(x < width && z < length && x > -1 && z > -1) {
            //west
            if(grid[(int) x][(int) (z)+1] != 1) {
                if(carPos.z%size >= size-2)
                {
                    carPos.z = z*size+size/2;
                    carSpeed.x = 0;
                    carSpeed.z = 0;
                    fov = normalfov;
                }
            }
            ///east
            if(z == 0 || grid[(int) x][(int) (z)-1] != 1) {
                if(carPos.z%size <= 2)
                {
                    carPos.z = z*size+size/2;
                    carSpeed.x = 0;
                    carSpeed.z = 0;
                    fov = normalfov;
                }
            }
            //south
            if(grid[(int) x+1][(int) (z)] != 1) {
                if(carPos.x%size >= size-2)
                {
                    carPos.x = x*size+size/2;
                    carSpeed.x = 0;
                    carSpeed.z = 0;
                    fov = normalfov;
                }
            }
            //north
            if(x == 0 || grid[(int) x-1][(int) (z)] != 1) {
                if(carPos.x%size < 2)
                {
                    carPos.x = x*size + size/2;
                    carSpeed.x = 0;
                    carSpeed.z = 0;
                    fov = normalfov;
                }
            }
        }
       
    }
	private void engineNoise() {
		engineRPM = carSpeed.length()/topSpeed[gear];
		if(shifting && gear > 0) {
			float prevGearRpm = carSpeed.length()/topSpeed[gear-1];
			engineRPM = this.lerp(prevGearRpm, engineRPM, (shift/shiftTime));
			engine.setVolume(noise, 0.4f+(engineRPM)/5);
			engine.setPitch(noise, 0.5f+(engineRPM));
		} else {
			engine.setVolume(noise, 0.4f+(engineRPM/5));
			engine.setPitch(noise, 0.5f+(engineRPM));
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
		engineNoise();			//Vroom vroom!
		time += deltaTime;		//Count the time
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
				
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

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
				shader.setHeadlightColor(0.5f, 0.4f, 0.35f, 1.0f);
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
				
				shader.setLightColor(0, 0, 0, 1);
				//Draw skybox
				shader.setMaterialEmission(0.4f, 0.4f, 0.4f, 1f);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(cam.eye.x, cam.eye.y, cam.eye.z);
				ModelMatrix.main.addScale(800, 800, 800);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.drawSolidCube(shader, skyBox);
				ModelMatrix.main.popMatrix();
				shader.setMaterialEmission(0, 0, 0, 1);
				
				shader.setLightColor(0.4f, 0.3f, 0.35f, 0);
		
				shader.setMaterialDiffuse(1.0f, 1.0f, 1.0f, 1.0f);
				shader.setMaterialSpecular(1.0f, 1.0f, 1.0f, 1.0f);
				shader.setMaterialAmbient(1, 1, 1, 1);
				shader.setMaterialShininess(50);
				
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
			
				for(int i = 0; i < width; i++)
                {
                    for(int k = 0; k < length; k++)
                    {
                        if(grid[i][k] == 1)
                        {
                            ModelMatrix.main.pushMatrix();
                            ModelMatrix.main.addTranslation(i*size+15, -2, k*size+15);
                            ModelMatrix.main.addScale(size, 1, size);
                            shader.setModelMatrix(ModelMatrix.main.getMatrix());
                            BoxGraphic.setUVArray(roadUV);
                            BoxGraphic.drawSolidCube(shader, road);
                            BoxGraphic.defaultUVArray();
                            ModelMatrix.main.popMatrix();
                        }  
                    }
                }
			} else {
				Gdx.gl.glViewport(2*Gdx.graphics.getWidth()/3, 0, Gdx.graphics.getWidth()/3, Gdx.graphics.getHeight()/3);
				camera.look(new Point3D(9.1f,100000.3f,9.2f), new Point3D(9.1f,100000.3f,7f), new Vector3D(0,10000,0));
				camera.perspectiveProjection(normalfov+10, (float)Gdx.graphics.getWidth() / (float)(Gdx.graphics.getHeight()), 0.2f, 100.0f);
				Rectangle scissors = new Rectangle(2*Gdx.graphics.getWidth()/3, 0, Gdx.graphics.getWidth()/3, Gdx.graphics.getHeight()/3);
				ScissorStack.pushScissors(scissors);
				//Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
				shader.setViewMatrix(camera.getViewMatrix());
				shader.setProjectionMatrix(camera.getProjectionMatrix());
				shader.setMaterialSpecular(0, 0, 0, 0);
				shader.setMaterialDiffuse(0, 0, 0, 0);
				shader.setMaterialAmbient(1, 1, 1, 1);
				shader.setMaterialEmission(0.8f, 0.8f, 0.8f, 1);
				
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(9.25f,100001.5f,7);
				ModelMatrix.main.addScale(0.19f, 0.19f, 0.19f);
				ModelMatrix.main.addRotationZ(120-accumulatedDriftBoost*4f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				hayai.draw(shader);
				ModelMatrix.main.popMatrix();
				
				shader.setMaterialEmission(1, 1, 1, 1);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(10.5f,100000,7);
				ModelMatrix.main.addScale(2f, 2f, 2f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.setUVArray(speedUV);
				BoxGraphic.drawSolidCube(shader, speedo);
				ModelMatrix.main.popMatrix();
				
				shader.setMaterialDiffuse(1, 1, 1,0.0f);
				shader.setMaterialAmbient(1, 1, 1, 0.0f);
				shader.setMaterialSpecular(1, 1, 1, 0.0f);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(10.5f,100000,7.01f);
				ModelMatrix.main.addScale(2f, 2f, 2f);
				ModelMatrix.main.addRotationZ(48-carSpeed.length()*2.5f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.drawSolidCube(shader, arrow);
				ModelMatrix.main.popMatrix();
				
				shader.setMaterialDiffuse(1, 1, 1,0.5f);
				shader.setMaterialAmbient(1, 1, 1, 0.5f);
				shader.setMaterialSpecular(1, 1, 1, 0.5f);
				shader.setMaterialEmission(0, 0, 0, 0);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(8f,100000,7);
				ModelMatrix.main.addScale(2f, 2f, 2f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.drawSolidCube(shader, tach);
				ModelMatrix.main.popMatrix();
				
				shader.setMaterialDiffuse(1, 1, 1,0.5f);
				shader.setMaterialAmbient(1, 1, 1, 0.5f);
				shader.setMaterialSpecular(1, 1, 1, 0.5f);
				shader.setMaterialEmission(1, 1, 1, 0);
				ModelMatrix.main.pushMatrix();
				ModelMatrix.main.addTranslation(8f,100000,7.01f);
				ModelMatrix.main.addScale(2f, 2f, 2f);
				ModelMatrix.main.addRotationZ(20-(engineRPM)*240f);
				shader.setModelMatrix(ModelMatrix.main.getMatrix());
				BoxGraphic.drawSolidCube(shader, arrow);
				BoxGraphic.defaultUVArray();
				ModelMatrix.main.popMatrix();
			
				ScissorStack.popScissors();
			}
		}
	}

	private float lerp(float a, float b, float f) 
	{
		return (a * (1.0f - f)) + (b * f);
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