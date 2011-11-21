uniform float uSteps;
uniform float uRadius;

vec4 texelIn(vec2 pos) {
    vec4 col = vec4(0.4, 0.1, 0.6, 1.0);
    if (length(vec2(-0.5, -0.5) - pos) < 0.05)
        col = vec4(vec3(2.0), 0.0);
    if (length(vec2(-0.5, 0.0) - pos) < 0.1)
        col = vec4(vec3(4.0), 0.6);
    if (length(vec2(-0.5, 0.5) - pos) < 0.1)
        col = vec4(vec3(4.0), 1.0);
    if (length(vec2(0.5, -0.5) - pos) < 0.1)
        col = vec4(vec3(1.0), 1.0);
    if (length(vec2(0.5, -0.5) - pos) < 0.05)
        col = vec4(vec3(0.5), 0.5);
    if (length(vec2(0.5, 0.5) - pos) < 0.1)
        col = vec4(vec3(0.5), 0.5);
    if (length(vec2(0.5, 0.5) - pos) < 0.05)
        col = vec4(vec3(1.0), 1.0);

    col.a = mix(0.01, 1.0, col.a);
    return vec4(col.rgb * col.a, col.a);
}

vec4 texelOut(vec4 color) {
    return vec4(color.rgb / color.a, color.a);
}

vec4 step1(vec2 delta, vec2 pos) {
    float coc = texelIn(pos).a;
    vec4 color = vec4(0.0);
    float sum = 0.0;
    for (float t = 0.0; t < uSteps; ++t) {
        vec4 sample = texelIn(pos);
        if (sample.a > t / uSteps)
        //if (abs(sample.a - coc) <= (uSteps - t) / uSteps)
        {
            float c = 1.0 - abs(sample.a - coc);
            color += sample * c;
            sum += c;
        }
        pos += delta;
    }
    color /= sum;
    return color;
}

vec4 step2(vec2 delta0, vec2 delta1, vec2 pos) {
    vec4 color0 = step1(delta0, pos - delta1 * 0.5);
    vec4 color1 = step1(delta1, pos - delta0 * 0.5);
    return (color0 + color1) / 2.0;
}

vec4 step3(vec2 delta0, vec2 delta1, vec2 delta2, vec2 pos) {
    float sum = 0.0;
    vec2 pos1 = pos - delta2 * 0.5;
    vec2 pos2 = pos;
    float coc1 = step1(delta0, pos1).a;
    float coc2 = step2(delta0, delta1, pos2).a;
    vec4 color = vec4(0.0);
    for (float t = 1.0; t <= uSteps; ++t) {
        vec4 sample1 = step1(delta0, pos1);
        vec4 sample2 = step2(delta0, delta1,  pos2);
        if (sample1.a > t / uSteps)
        //if (abs(sample1.a - coc1) <= (uSteps - t) / uSteps)
        {
            float c = 1.0 - abs(sample1.a - coc1);
            color += sample1 * c;
            sum += c;
        }
        if (sample2.a > t / uSteps)
        //if (abs(sample2.a - coc2) <= (uSteps - t) / uSteps)
        {
            float c = 2.0 * (1.0 - abs(sample2.a - coc2));
            color += sample2 * c;
            sum += c;
        }
        pos1 += delta1;
        pos2 += delta2;
    }
    return color / sum;
}

void main()
{
     float radius = uRadius / uSteps;
     vec2 dir0 = vec2(0.0, 1.0) * radius;
     vec2 dir1 = vec2(-0.866, -0.5) * radius;
     vec2 dir2 = vec2(0.866, -0.5) * radius;

     vec2 pos = gl_TexCoord[0].xy;
     pos -= 0.5;
     pos *= 2.0;

     vec4 blurredCol = texelIn(pos);
     blurredCol = step1(dir0, pos);
     blurredCol = step2(dir0, dir1, pos);
     blurredCol = step3(dir0, dir1, dir2, pos);
     gl_FragColor = texelOut(blurredCol);
     gl_FragColor.rgb /= 4.0;
     gl_FragColor = mix(texelOut(texelIn(pos)), gl_FragColor, gl_FragColor.a);
     gl_FragColor.a = 1.0;
}

