#version 400 core

in vec3 texCoords;

out vec4 outColor;

uniform samplerCube skyboxTexture;

void main() {
    outColor = texture(skyboxTexture, texCoords);
}
