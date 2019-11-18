#version 410


in vec3 v_normal;
in vec3 FragPos;

uniform vec4 u_lightDir;

uniform sampler2D u_diffuseTexture;
in vec2 v_texcoord;

layout(location = 0) out vec4 colour;

const float GAMMA = 1.9;

void main() {
    vec3 normal = normalize(v_normal);
    vec3 lightDir = normalize(u_lightDir.xyz);
    vec4 dColor = texture(u_diffuseTexture, v_texcoord);
    dColor = pow(dColor, vec4(GAMMA));
    vec4 diffuse = dColor * max(0, dot(lightDir, normal));
    diffuse = pow(diffuse, vec4(1.0/GAMMA));
    vec3 diffuse3 = vec3(diffuse);
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * vec3(1,1,1);
    vec3 result = diffuse3 + ambient * vec3(dColor);
    colour = vec4(result, 1);
}

