#version 400 core

layout(location = 0) in vec3 vertexPosition;

out vec3 texCoords;

uniform mat4 VPTransform;

void main() {
    texCoords = vertexPosition;
    vec4 pos = VPTransform * vec4(vertexPosition, 1.0);

    // ukoliko ne bismo prosledili w kao z, skybox bi imao normalnu dubinu
    // zbog toga, bio bi delimicno prekriven Zemljom ili odsecen far plane-om
    gl_Position = pos.xyww;  
}
