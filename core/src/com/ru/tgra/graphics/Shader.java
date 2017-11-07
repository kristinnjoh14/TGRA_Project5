package com.ru.tgra.graphics;

import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;

public class Shader {
	Shader shader;
	
	private int renderingProgramID;
	private int vertexShaderID;
	private int fragmentShaderID;
	
	private boolean usesDiffuseTexture = false;
	private int usesDiffuseTexLoc;
	private int diffuseTextureLoc;

	private int eyePositionLoc;
	private int positionLoc;
	private int normalLoc;
	private int uvLoc;

	private int modelMatrixLoc;
	private int viewMatrixLoc;
	private int projectionMatrixLoc;

	private int globalAmbientLoc;
	private int materialAmbientLoc;
	private int materialDiffuseLoc;
	private int matEmissionLoc;
	
	private int lightDiffuseLoc;
	private int materialSpecularLoc;
	private int materialShininessLoc;
	private int lightSpecularLoc;
	private int lightPositionLoc;
	
	private int headlightSpecularLoc;
	private int headlightDiffuseLoc;
	private int leftHeadlightPositionLoc;
	private int rightHeadlightPositionLoc;
	private int headlightDirectionLoc;
	private int linearAttenuationLoc;
	private int quadraticAttenuationLoc;
	private int constantAttenuationLoc;
	private int headlightExponentLoc;
	
	public Shader() {
		String vertexShaderString;
		String fragmentShaderString;

		vertexShaderString = Gdx.files.internal("shaders/fragmentLighting3D.vert").readString();
		fragmentShaderString =  Gdx.files.internal("shaders/fragmentLighting3D.frag").readString();

		vertexShaderID = Gdx.gl.glCreateShader(GL20.GL_VERTEX_SHADER);
		fragmentShaderID = Gdx.gl.glCreateShader(GL20.GL_FRAGMENT_SHADER);
	
		Gdx.gl.glShaderSource(vertexShaderID, vertexShaderString);
		Gdx.gl.glShaderSource(fragmentShaderID, fragmentShaderString);
	
		Gdx.gl.glCompileShader(vertexShaderID);
		Gdx.gl.glCompileShader(fragmentShaderID);

		System.out.println("Vertex shader");
		System.out.println(Gdx.gl.glGetShaderInfoLog(vertexShaderID));
		System.out.println("Fragment shader");
		System.out.println(Gdx.gl.glGetShaderInfoLog(fragmentShaderID));
		
		renderingProgramID = Gdx.gl.glCreateProgram();
	
		Gdx.gl.glAttachShader(renderingProgramID, vertexShaderID);
		Gdx.gl.glAttachShader(renderingProgramID, fragmentShaderID);
	
		Gdx.gl.glLinkProgram(renderingProgramID);

		positionLoc				= Gdx.gl.glGetAttribLocation(renderingProgramID, "a_position");
		Gdx.gl.glEnableVertexAttribArray(positionLoc);

		normalLoc				= Gdx.gl.glGetAttribLocation(renderingProgramID, "a_normal");
		Gdx.gl.glEnableVertexAttribArray(normalLoc);

		uvLoc				= Gdx.gl.glGetAttribLocation(renderingProgramID, "a_uv");
		Gdx.gl.glEnableVertexAttribArray(uvLoc);
		
		modelMatrixLoc			= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_modelMatrix");
		viewMatrixLoc			= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_viewMatrix");
		projectionMatrixLoc	= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_projectionMatrix");

		eyePositionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_eyePosition");
		
		materialAmbientLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_materialAmbient");
		materialDiffuseLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_materialDiffuse");
		materialSpecularLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_materialSpecular");
		materialShininessLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_materialShininess");
		matEmissionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_materialEmission");
		
		globalAmbientLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_globalAmbient");
		lightDiffuseLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_lightDiffuse");
		lightSpecularLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_lightSpecular");
		lightPositionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_lightPosition");
		
		headlightSpecularLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_headlightSpecular");
		leftHeadlightPositionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_leftHeadlightPosition");
		rightHeadlightPositionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_rightHeadlightPosition");

		headlightDiffuseLoc					= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_headlightDiffuse");
		headlightDirectionLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_headlightDirection");
		headlightExponentLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_headlightExponent");
		linearAttenuationLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_linearAttenuation");
		quadraticAttenuationLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_quadraticAttenuation");
		constantAttenuationLoc				= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_constantAttenuation");
		
		usesDiffuseTexLoc					= Gdx.gl.glGetUniformLocation(renderingProgramID, "u_usesDiffuseTexture");
		diffuseTextureLoc					=Gdx.gl.glGetUniformLocation(renderingProgramID, "u_diffuseTexture");
		Gdx.gl.glUseProgram(renderingProgramID);
	}
	
	public void setDiffuseTexture(Texture tex)
	{
		if(tex == null)
		{
			Gdx.gl.glUniform1f(usesDiffuseTexLoc, 0.0f);
			usesDiffuseTexture = false;
		}
		else
		{
			tex.bind(0);
			Gdx.gl.glUniform1i(diffuseTextureLoc, 0);
			Gdx.gl.glUniform1f(usesDiffuseTexLoc, 1.0f);
			usesDiffuseTexture = true;

			Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_REPEAT);
			Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_REPEAT);
		}
	}

	public boolean usesTextures()
	{
		return (usesDiffuseTexture/* || usesSpecularTexture ... etc.*/);
	}
	
	public void setGlobalAmbient(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(globalAmbientLoc, r, g, b, a);
	}
	
	public void setMaterial(Vector3D ambient, Vector3D specular, Vector3D diffuse, int shininess) {
		this.setMaterialAmbient(ambient.x, ambient.y, ambient.z, 1);
		this.setMaterialSpecular(specular.x, specular.y, specular.z, 1);
		this.setMaterialDiffuse(diffuse.x, diffuse.y, diffuse.z, 1);
		this.setMaterialShininess(shininess);
	}
	
	public void setMaterialAmbient(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(materialAmbientLoc, r, g, b, a);
	}
	
	public void setMaterialSpecular(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(materialSpecularLoc, r, g, b, a);
	}
	
	public void setMaterialShininess(int shininess) {
		Gdx.gl.glUniform1f(materialShininessLoc, shininess);
	}
	
	public void setMaterialDiffuse(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(materialDiffuseLoc, r, g, b, a);
	}
	
	public void setMaterialEmission(float r, float g, float b, float a)
	{
		Gdx.gl.glUniform4f(matEmissionLoc, r, g, b, a);
	}
	
	public void setLightSpecular(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(lightSpecularLoc, r, g, b, a);
	}
	
	public void setLightDiffuse(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(lightDiffuseLoc, r, g, b, a);
	}
	
	public void setLightColor(float r, float g, float b, float a) {
		this.setLightDiffuse(r, g, b, a);
		this.setLightSpecular(r, g, b, a);
	}
	
	public void setLightPosition(float x, float y, float z, float w) {
		Gdx.gl.glUniform4f(lightPositionLoc, x, y, z, w);
	}
	
	public void setHeadlightSpecular(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(headlightSpecularLoc, r, g, b, a);
	}
	
	public void setHeadlightDiffuse(float r, float g, float b, float a) {
		Gdx.gl.glUniform4f(headlightDiffuseLoc, r, g, b, a);
	}
	
	public void setHeadlightColor(float r, float g, float b, float a) {
		this.setHeadlightDiffuse(r, g, b, a);
		this.setHeadlightSpecular(r, g, b, a);
	}
	
	public void setLeftHeadlightPosition(float x, float y, float z, float w) {
		Gdx.gl.glUniform4f(leftHeadlightPositionLoc, x, y, z, w);
	}
	
	public void setRightHeadlightPosition(float x, float y, float z, float w) {
		Gdx.gl.glUniform4f(rightHeadlightPositionLoc, x, y, z, w);
	}
	
	public void setHeadlightDirection(float x, float y, float z, float w) {
		Gdx.gl.glUniform4f(headlightDirectionLoc, x, y, z, w);
	}
	
	public void setLinearAttenuation(float r) {
		Gdx.gl.glUniform1f(linearAttenuationLoc, r);
	}
	
	public void setQuadraticAttenuation(float r) {
		Gdx.gl.glUniform1f(quadraticAttenuationLoc, r);
	}
	
	public void setConstantAttenuation(float r) {
		Gdx.gl.glUniform1f(constantAttenuationLoc, r);
	}
	
	public void setHeadlightExponent(float r) {
		Gdx.gl.glUniform1f(headlightExponentLoc, r);
	}
	
	public void setEyePosition(Point3D eye, float w) {
		Gdx.gl.glUniform4f(eyePositionLoc, eye.x, eye.y, eye.z, w);
	}
	
	public int getVertexPointer() {
		return positionLoc;
	}
	
	public int getNormalPointer() {
		return normalLoc;
	}
	
	public int getUVPointer()
	{
		return uvLoc;
	}
	
	public void setModelMatrix(FloatBuffer matrix) {
		Gdx.gl.glUniformMatrix4fv(modelMatrixLoc, 1, false, matrix);
	}
	
	public void setViewMatrix(FloatBuffer matrix) {
		Gdx.gl.glUniformMatrix4fv(viewMatrixLoc, 1, false, matrix);
	}
	
	public void setProjectionMatrix(FloatBuffer matrix) {
		Gdx.gl.glUniformMatrix4fv(projectionMatrixLoc, 1, false, matrix);
	}
}

