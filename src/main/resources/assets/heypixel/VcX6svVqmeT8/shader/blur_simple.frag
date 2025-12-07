#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform float uBlurStrength;
uniform vec2 uResolution;

void main() {
    vec2 texelSize = 1.0 / uResolution;
    vec4 result = vec4(0.0);
    
    float blurSize = uBlurStrength * texelSize.x;
    
    for(int i = -2; i <= 2; i++) {
        for(int j = -2; j <= 2; j++) {
            vec2 offset = vec2(float(i), float(j)) * blurSize;
            result += texture(uTexture, uv + offset);
        }
    }
    
    color = result / 25.0;
    color.a = 1.0;
}