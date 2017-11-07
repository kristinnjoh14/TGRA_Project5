#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 u_eyePosition;

uniform vec4 u_globalAmbient;
uniform vec4 u_materialAmbient;
uniform vec4 u_materialEmission;

uniform vec4 u_lightPosition;
uniform vec4 u_lightDiffuse;
uniform vec4 u_lightSpecular;

uniform vec4 u_materialDiffuse;
uniform float u_materialShininess;
uniform vec4 u_materialSpecularity;

uniform vec4 u_leftHeadlightPosition;
uniform vec4 u_rightHeadlightPosition;
uniform vec4 u_headlightDirection;
uniform vec4 u_headlightDiffuse;
uniform vec4 u_headlightSpecular;

uniform sampler2D u_diffuseTexture;
uniform float u_usesDiffuseTexture;

uniform float u_quadraticAttenuation;
uniform float u_constantAttenuation;
uniform float u_linearAttenuation;
uniform float u_headlightExponent;

varying vec4 v_normal;
varying vec4 v_s;
varying vec4 v_h;
varying vec4 v_s2;
varying vec4 v_s3;
varying vec2 v_uv;

void main()
{
	vec4 materialDiffuse;
	if(u_usesDiffuseTexture == 1.0)
	{
		materialDiffuse = texture2D(u_diffuseTexture, v_uv);  //also * u_materialDiffuse ??? up to you.
	}
	else
	{
		materialDiffuse = u_materialDiffuse;
	}

	float lambert = max(0.0, dot(v_normal, v_s) / (length(v_normal)*length(v_s)));
	float phong = max(0.0, dot(v_normal, v_h) / (length(v_normal)*length(v_h)));
	
	gl_FragColor = lambert*u_lightDiffuse*materialDiffuse;
	gl_FragColor += u_globalAmbient*u_materialAmbient + u_materialEmission;
	gl_FragColor += pow(phong, u_materialShininess)*u_lightSpecular*u_materialSpecularity;
	
	for(int i = 0; i < 2; i++) {
		float lengths2;
		if(i == 0) {
			lengths2 = length(v_s2);
		} else {
			lengths2 = length(v_s3);
		}
		float flashAttenuation = max(0.0, (dot(-v_s2, u_headlightDirection)/(lengths2*length(u_headlightDirection))));
		flashAttenuation = pow(flashAttenuation, u_headlightExponent);
		float distanceAttenuation = 1.0/(u_constantAttenuation + lengths2*u_linearAttenuation + pow(lengths2,2.0)*u_quadraticAttenuation);

		vec4 res = distanceAttenuation*flashAttenuation*((u_headlightDiffuse*materialDiffuse)+(u_headlightSpecular*u_materialSpecularity));
		gl_FragColor += res;
	}
	gl_FragColor *= materialDiffuse;
}