#version 410

in vec4 gl_FragCoord;

in vec3 v_normal;
in vec3 FragPos;

uniform vec4 u_lightDir;
uniform vec4 u_viewDir; 
uniform vec4 u_specularMaterial;

layout(location = 0) out vec4 colour;

float specularStrength = 2;
float shininess = 32;
vec3 lightColour = vec3(1, 1, 1);

vec3 phongModel(vec3 normal, vec3 lightDir) {
    vec3 viewDir = normalize(u_viewDir.xyz);    
	vec3 reflected = reflect(lightDir, normal);  
    vec3 result = lightColour * pow(max(0,dot(reflected, viewDir)), shininess) * specularStrength ;
    return result;
}

void main() {
    vec3 normal = normalize(v_normal);
    float freq = 20;
    float x = FragPos.x * freq;
    float d = sqrt(1+pow(cos(x),2)/64);
    normal.x = (-cos(x)/8)/d;
    normal.y = 1/d;
    normal.z = 0;
    normal = normalize(normal);
    vec3 lightDir = normalize(u_lightDir.xyz - FragPos);
    vec3 specular = vec3(0);
    specular = phongModel(normal, lightDir);
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * vec3(1,1,1);
    vec3 diffuse = lightColour * max(0, dot(lightDir, normal));
    vec3 rgb = specular + (diffuse + ambient ) * vec3(u_specularMaterial);
    float c = (normal.z )/2;
   	colour = vec4(rgb, u_specularMaterial.w);
}
