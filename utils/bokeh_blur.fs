uniform sampler2D sTexture0;
uniform float uSteps;
uniform float uRadius;

vec4 texelIn(vec2 pos) {
    vec4 col = vec4(0.4, 0.1, 0.6, 1.0);

    if (length(vec2(-0.5, -0.5) - pos) < 0.05)
        col = vec4(vec3(3.0), 0.0);

    if (length(vec2(-0.5, 0.0) - pos) < 0.1)
        col = vec4(vec3(3.0), 0.6);

    if (length(vec2(-0.5, 0.5) - pos) < 0.1)
        col = vec4(vec3(3.0), 1.0);

    if (length(vec2(0.5, -0.5) - pos) < 0.1)
        col = vec4(vec3(3.0), 1.0);
    if (length(vec2(0.5, -0.5) - pos) < 0.05)
        col = vec4(vec3(3.0), 0.0);

    if (length(vec2(0.5, 0.5) - pos) < 0.1)
        col = vec4(vec3(3.0), 0.0);
    if (length(vec2(0.5, 0.5) - pos) < 0.05)
        col = vec4(vec3(3.0), 1.0);

    col.a = mix(0.01, 1.0, col.a);
    return vec4(col.rgb * col.a, col.a);
}

vec4 texelOut(vec4 color) {
    return vec4(color.rgb / color.a, color.a);
}

vec4 step1(vec2 delta, vec2 pos) {
    vec4 color = texelIn(pos);
    float coc = color.a;
    float sum = 1.0;
    for (float t = 1.0; coc * uSteps > t; ++t) {
        pos += delta;
        vec4 sample = texelIn(pos);
        if (sample.a * uSteps > t) {
            color += sample;
            sum += 1.0;
        }
    }
    return color / sum;
}

vec4 step2(vec2 delta0, vec2 delta1, vec2 pos) {
    vec4 color0 = step1(delta0, pos - delta1 * 0.5);
    vec4 color1 = step1(delta1, pos - delta0 * 0.5);
    return (color0 + color1) / 2.0;
}

vec4 step3(vec2 delta0, vec2 delta1, vec2 pos) {
    vec4 color = step1(delta0, pos);
    float coc = color.a;
    float sum = 1.0;
    for (float t = 1.0; coc * uSteps > t; ++t) {
        vec4 sample = step1(delta0, pos);
        if (sample.a * uSteps > t) {
            color += sample;
            sum += 1.0;
        }
        pos += delta1;
    }
    return color / sum;
}

vec4 step4(vec2 delta0, vec2 delta1, vec2 delta2, vec2 pos) {
    float sum = 1.0;
    vec4 color = step2(delta0, delta1, pos);
    float coc = color.a;
    for (float t = 1.0; coc * uSteps > t; ++t) {
        vec4 sample = step2(delta0, delta1,  pos);
        if (sample.a * uSteps > t) {
            color += sample;
            sum += 1.0;
        }
        pos += delta2;
    }
    return color / sum;
}

vec4 step5(vec2 delta0, vec2 delta1, vec2 delta2, vec2 pos) {
    vec4 color0 = step3(delta0, delta1, pos - delta2 * 0.5);
    vec4 color1 = step4(delta0, delta1, delta2, pos);
    return (color0 + 2.0 * color1) / 3.0;
}

void main()
{
     float radius = uRadius / uSteps;
     vec2 dir0 = vec2(0.0, 1.0) * radius;
     vec2 dir1 = vec2(-0.866, -0.5) * radius;
     vec2 dir2 = vec2(0.866, -0.5) * radius;

     vec2 pos = gl_TexCoord[0].xy;
     pos *= 2.0;
     pos -= 1.0;

     vec4 blurredCol = texelIn(pos);
     blurredCol = step1(dir0, pos);
     blurredCol = step2(dir0, dir1, pos);
     blurredCol = step3(dir0, dir1, pos);
     blurredCol = step4(dir0, dir1, dir2, pos);
     blurredCol = step5(dir0, dir1, dir2, pos);
     gl_FragColor = texelOut(blurredCol);
     gl_FragColor = mix(texelOut(texelIn(pos)), gl_FragColor, gl_FragColor.a);
     gl_FragColor.a = 1.0;
}

