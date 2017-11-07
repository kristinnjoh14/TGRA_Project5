#ifdef GL_ES
precision mediump float;
#endif

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_uv;

uniform mat4 u_modelMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;

uniform vec4 u_eyePosition;

uniform vec4 u_globalAmbient;
uniform vec4 u_materialAmbient;

uniform vec4 u_lightDiffuse;
uniform vec4 u_materialDiffuse;
uniform float u_materialShininess;
uniform vec4 u_materialSpecularity;
uniform vec4 u_lightSpecular;
uniform vec4 u_headlightDiffuse;
uniform vec4 u_headlightSpecular;

uniform vec4 u_lightPosition;
uniform vec4 u_leftHeadlightPosition;
uniform vec4 u_rightHeadlightPosition;
uniform vec4 u_headlightDirection;

uniform float u_quadraticAttenuation;
uniform float u_constantAttenuation;
uniform float u_linearAttenuation;

varying vec4 v_normal;
varying vec4 v_s;
varying vec4 v_h;
varying vec4 position;
varying vec4 v_s2;
varying vec4 v_s3;
varying vec2 v_uv;

void main()
{
	position = vec4(a_position.x, a_position.y, a_position.z, 1.0);
	position = u_modelMatrix * position;

	v_s2 = u_leftHeadlightPosition - position;
	v_s3 = u_rightHeadlightPosition - position;
	
	vec4 normal = vec4(a_normal.x, a_normal.y, a_normal.z, 0.0);
	normal = u_modelMatrix * normal;
	
	v_normal = normal;

	position = u_viewMatrix * position;
	
	vec4 v = u_eyePosition - position;
	v_s = u_lightPosition - position;
	v_h = v_s + v;
	
	v_uv = a_uv;
	
	gl_Position = u_projectionMatrix * position;
}