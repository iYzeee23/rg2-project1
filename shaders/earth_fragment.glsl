#version 400 core

in vec2 fragUV;
in vec3 fragPosEyeSpace;
in mat3 TBN;

out vec4 outColor;

uniform vec3 LightPosition;
uniform sampler2D earthTexture;
uniform sampler2D normalMap;
uniform sampler2D specularMap;

void main() {
    // JPG slika nema kanal, tj uvek je neprozirna (JPG je compressed)
    // PNG moze da ima transparentnost (PNG je lossless)
    vec3 earthColor = texture(earthTexture, fragUV).rgb;

    // grayscale slika (crno bela), pa je svaki kanal isti
    float specIntensity = texture(specularMap, fragUV).r;

    // konverzija iz tangent prostora u prostor kamere
    // texture uvek vraca normalizovane vrednosti, a ne od 0 do 255
    // normal tangent == normala u tangent space-u
    vec3 normalTangent = texture(normalMap, fragUV).rgb * 2.0 - 1.0;
    vec3 normal = normalize(TBN * normalTangent);

    // svetlo je vec u eye space-u, ne treba transformisati
    vec3 lightPosEye = LightPosition;

    // smerovi za osvetljenje (krajnja tacka - pocetna tacka)
    vec3 lightVec = normalize(lightPosEye - fragPosEyeSpace);
    vec3 viewVec  = normalize(-fragPosEyeSpace);
    vec3 reflected = reflect(-lightVec, normal);

    // fongov model sencenja
    float ambient  = 0.08;
    float diffuse  = max(dot(normal, lightVec), 0.0);
    float specular = pow(max(dot(viewVec, reflected), 0.0), 32.0) * specIntensity;

    // https://learnopengl.com/Lighting/Basic-Lighting
    vec3 finalColor = (ambient + diffuse) * earthColor + specular * vec3(1.0);
    outColor = vec4(finalColor, 1.0);
}
