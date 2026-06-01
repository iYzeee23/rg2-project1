#version 400 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 vertexUV;
layout(location = 2) in vec3 vertexNormal;
layout(location = 3) in vec3 vertexTangent;

out vec2 fragUV;
out vec3 fragPosEyeSpace;
out mat3 TBN;

uniform mat4 MVPTransform;
uniform mat4 MVTransform;
uniform mat3 NormalTransform;

void main() {
    // https://learnopengl.com/Getting-started/Coordinate-Systems
    fragUV = vertexUV;
    fragPosEyeSpace = (MVTransform * vec4(vertexPosition, 1.0)).xyz;
    gl_Position = MVPTransform * vec4(vertexPosition, 1.0);

    // tutorial:  T = normalize(model * vec4(aTangent, 0.0))
    // nasa impl: T = normalize(NormalTransform * vertexTangent)
    // razlika: mi koristimo NormalTransform = transpose(inverse(MV))
    vec3 N = normalize(NormalTransform * vertexNormal);
    vec3 T = normalize(NormalTransform * vertexTangent);

    // gram-schmidt: T = T - (T . N) * N
    // optimizacija koja osigurava da su T i N ortogonalni
    T = normalize(T - dot(T, N) * N);

    // tutorial: aBitangent kao ulaz
    // nasa impl: racunamo kao B = N x T
    vec3 B = cross(N, T);

    // https://learnopengl.com/Advanced-Lighting/Normal-Mapping
    TBN = mat3(T, B, N);
}
