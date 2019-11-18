#version 410

in vec3 a_position;
in vec3 a_normal;
out vec3 FragPos;

uniform mat4 u_mvpMatrix;
uniform mat3 u_normalMatrix;
uniform mat4 u_worldMatrix;

out vec3 v_normal;

void main() {
    gl_Position = u_mvpMatrix * vec4(a_position, 1);

    FragPos = vec3(u_worldMatrix * vec4 (a_position, 1.0));
    
    vec3 normal = u_normalMatrix * a_normal;

    v_normal = normal;    
}

