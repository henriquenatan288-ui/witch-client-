#version 120

// Witch Mono - post-processing monocromatico (nao mexe na geometria do jogo).

// ===== opcoes (aparecem no menu do Iris) =====
#define MONO 0.35          // [0.0 0.1 0.2 0.3 0.35 0.4 0.5 0.6 0.7 0.8 0.9 1.0]
#define CONTRAST 1.15      // [1.0 1.05 1.1 1.15 1.2 1.3 1.4]
#define BRIGHTNESS 0.0     // [-0.1 -0.05 0.0 0.05 0.1]
#define VIGNETTE 0.45      // [0.0 0.15 0.3 0.45 0.6 0.75 0.9]
#define GRAIN 0.025        // [0.0 0.01 0.025 0.04 0.06]
#define BLOOM 0.35         // [0.0 0.15 0.25 0.35 0.5 0.7 1.0]

uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;

varying vec2 texcoord;

float luma(vec3 c) {
	return dot(c, vec3(0.299, 0.587, 0.114));
}

float hash(vec2 p) {
	return fract(sin(dot(p, vec2(12.9898, 78.233)) + frameTimeCounter) * 43758.5453);
}

// brilho suave: soma amostras vizinhas so das partes claras
vec3 bloom(vec2 uv) {
	vec2 px = 1.0 / vec2(viewWidth, viewHeight);
	vec3 sum = vec3(0.0);
	float total = 0.0;
	for (int i = -2; i <= 2; i++) {
		for (int j = -2; j <= 2; j++) {
			vec2 o = vec2(float(i), float(j)) * px * 3.0;
			vec3 s = texture2D(colortex0, uv + o).rgb;
			float w = 1.0 / (1.0 + float(i * i + j * j));
			float bright = max(luma(s) - 0.6, 0.0);
			sum += s * bright * w;
			total += w;
		}
	}
	return sum / total;
}

void main() {
	vec3 c = texture2D(colortex0, texcoord).rgb;

	if (BLOOM > 0.0) {
		c += bloom(texcoord) * BLOOM * 2.0;
	}

	float l = luma(c);
	c = mix(c, vec3(l), MONO);

	c = (c - 0.5) * CONTRAST + 0.5 + BRIGHTNESS;

	vec2 d = texcoord - 0.5;
	float v = smoothstep(0.85, 0.2, length(d * vec2(1.0, viewHeight / viewWidth + 0.35)));
	c *= mix(1.0 - VIGNETTE, 1.0, v);

	c += (hash(texcoord * vec2(viewWidth, viewHeight)) - 0.5) * GRAIN;

	gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
