#version 150

#moj_import <fog.glsl>
#moj_import <projection.glsl>
#moj_import <globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec2 ScreenSize;
uniform sampler2D Sampler2;

out vec4 vertexColor;
out vec2 texCoord0;
out float vertexDistance;

#define HUD_MAX_ID ${HUD_MAX_ID}

void main() {
    vec3 pos = Position;
    vec2 ui = ceil(2 / vec2(ProjMat[0][0], -ProjMat[1][1]));
    vec2 uiScreen = ui / ScreenSize;

    float xGui = 0.0;
    float yGui = 0.0;
    float layer = 0.0;

    int id = (int(Color.r * 255.0) << 16) | (int(Color.g * 255.0) << 8) | int(Color.b * 255.0);
    if (id >= 1 && id <= HUD_MAX_ID) {
        switch (id) {
            #CreateLayout
        }
        pos.x += xGui;
        pos.y += yGui;
        pos.z += layer;
        vertexColor = vec4(1.0, 1.0, 1.0, Color.a) * texelFetch(Sampler2, UV2 / 16, 0);
    } else {
        vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    }

    vertexDistance = fog_distance(ModelViewMat, pos, FogShape);
    texCoord0 = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}
