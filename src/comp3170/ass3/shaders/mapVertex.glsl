#version 410

in vec3 a_position;
in vec2 a_texcoord;
out vec2 v_texcoord;
out vec3 FragPos;

in vec3 a_normal;
uniform mat4 u_worldMatrix;
uniform mat3 u_normalMatrix;

uniform mat4 u_mvpMatrix;
out vec3 v_normal;

void main() {
    gl_Position = u_mvpMatrix * vec4(a_position,1);
    FragPos = vec3(u_worldMatrix * vec4 (a_position, 1.0));
    v_texcoord = a_texcoord;

    vec3 normal = u_normalMatrix * a_normal;

    v_normal = normal;
}

