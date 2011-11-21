Utility files used during development. All of these require Apple OSX and/or XCode installed.

- <b>coc_calculator.gcx</b> is an OSX Grapher project used to evaluate different
circle of confusion values needed for lens blur.
- <b>bokeh_blur.sbproj</b> is an OpenGL Shader Builder project which is used to
develop lens blur. Due to the nature of Shader Builder, blur is implemented as a
very heavy recursion within one fragment shader.