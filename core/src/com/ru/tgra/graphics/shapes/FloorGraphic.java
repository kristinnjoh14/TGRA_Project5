package com.ru.tgra.graphics.shapes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;
import com.ru.tgra.graphics.Shader;

public class FloorGraphic {

	private static FloatBuffer vertexBuffer;
	private static FloatBuffer normalBuffer;
	private static FloatBuffer uvBuffer;
	private static ShortBuffer indexBuffer;

	public static void create() {

		//VERTEX ARRAY IS FILLED HERE
		float[] vertexArray = {-0.5f, 0.5f, -0.5f,
						0.5f, 0.5f, -0.5f,
						0.5f, 0.5f, 0.5f,
						-0.5f, 0.5f, 0.5f};

		vertexBuffer = BufferUtils.newFloatBuffer(12);
		BufferUtils.copy(vertexArray, 0, vertexBuffer, 12);
		vertexBuffer.rewind();
		System.out.println(vertexBuffer);

		//NORMAL ARRAY IS FILLED HERE
		float[] normalArray = {0.0f, 0.0f, 1.0f,
							0.0f, 0.0f, 1.0f,
							0.0f, 0.0f, 1.0f,
							0.0f, 0.0f, 1.0f};

		normalBuffer = BufferUtils.newFloatBuffer(12);
		BufferUtils.copy(normalArray, 0, normalBuffer, 12);
		normalBuffer.rewind();


		defaultUVArray();


		//INDEX ARRAY IS FILLED HERE
		short[] indexArray = {0, 1, 2, 0, 2, 3};

		indexBuffer = BufferUtils.newShortBuffer(6);
		BufferUtils.copy(indexArray, 0, indexBuffer, 6);
		indexBuffer.rewind();

	}
	
	public static void defaultUVArray() {
		//UV TEXTURE COORD ARRAY IS FILLED HERE
		float[] uvArray = {0.0f, 0.0f,		//Left
						1.0f, 0.0f,
						1.0f, 1.0f,
						0.0f, 1.0f};
		
		uvBuffer = BufferUtils.newFloatBuffer(8);
		BufferUtils.copy(uvArray, 0, uvBuffer, 8);
		uvBuffer.rewind();
	}
	
	public static void setUVArray(float[] uv) {
		uvBuffer = BufferUtils.newFloatBuffer(8);
		BufferUtils.copy(uv, 0, uvBuffer, 8);
		uvBuffer.rewind();
	}

	public static void drawSolidPlane(Shader shader, Texture diffuseTexture) {

		shader.setDiffuseTexture(diffuseTexture);
		System.out.println(vertexBuffer);
		Gdx.gl.glVertexAttribPointer(shader.getVertexPointer(), 3, GL20.GL_FLOAT, false, 0, vertexBuffer);
		Gdx.gl.glVertexAttribPointer(shader.getNormalPointer(), 3, GL20.GL_FLOAT, false, 0, normalBuffer);
		Gdx.gl.glVertexAttribPointer(shader.getUVPointer(), 2, GL20.GL_FLOAT, false, 0, uvBuffer);

		Gdx.gl.glDrawElements(GL20.GL_TRIANGLES, 6, GL20.GL_UNSIGNED_SHORT, indexBuffer);
	}

}
