#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 playerCanvas;
flat out int playerModelType;
flat out int playerModelState;
flat out float playerModelPhase;
flat out int playerModelAnimation;

// Preserve Nexo's animated-font handling when this shader replaces its active version overlay.
#moj_import <minecraft:nexo_gif_utils.glsl>

void main() {
    int red = int(round(Color.r * 255.0));
    int green = int(round(Color.g * 255.0));
    int blue = int(round(Color.b * 255.0));
    bool markerEncoding = green / 16 <= 2 && blue / 16 >= 1;
    bool baseSkinLayer = UV0.x >= 0.12 && UV0.x <= 0.26 && UV0.y >= 0.12 && UV0.y <= 0.26;
    bool hatSkinLayer = UV0.x >= 0.62 && UV0.x <= 0.76 && UV0.y >= 0.12 && UV0.y <= 0.26;
    bool playerSkinLayer = baseSkinLayer || hatSkinLayer;
    // GUI batching can expose the dynamically bound skin through Sampler0 too late for a reliable
    // textureSize() query in the vertex stage. The reserved RGB protocol is sufficient by itself.
    bool modelMarker = red / 16 == 7 && markerEncoding && baseSkinLayer;
    bool duplicateHatMarker = red / 16 == 7 && markerEncoding && hatSkinLayer;
    // Minecraft quarters RGB for text shadows. Match both skin quads so the hat-layer shadow
    // cannot survive as the small head that used to appear in the tooltip's top-left corner.
    bool shadowMarker = red >= 26 && red <= 33 && playerSkinLayer;

    vec3 position = Position;
    playerCanvas = vec2(0.0);
    playerModelType = -1;
    playerModelState = (shadowMarker || duplicateHatMarker) ? -1 : 0;
    playerModelPhase = 0.0;
    playerModelAnimation = -1;

    if (modelMarker) {
        int type = clamp(green / 16, 0, 2);
        int yOffset = green % 16;
        int scale = clamp(blue / 16, 1, 15);
        int speedStep = red % 16;
        float speed = speedStep == 15 ? 4.0 : float(speedStep) * 0.25;
        int vertex = gl_VertexID & 3;
        vec2 corner = vec2(vertex >= 2 ? 1.0 : 0.0, (vertex == 1 || vertex == 2) ? 1.0 : 0.0);
        vec2 modelUnits = type == 0 ? vec2(10.0, 10.0) : (type == 1 ? vec2(18.0, 22.0) : vec2(18.0, 34.0));
        vec2 pixelSize = modelUnits * float(scale);

        position.xy += corner * (pixelSize - vec2(8.0));
        position.y += float(yOffset);
        playerCanvas = corner;
        playerModelType = type;
        playerModelState = 1;
        playerModelPhase = GameTime * 24000.0 * speed;
        playerModelAnimation = blue % 16;
    } else if (shadowMarker || duplicateHatMarker) {
        // The enlarged model supplies its own shading; suppress the vanilla 8x8 text shadow.
        position.xy = vec2(-100000.0);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    sphericalVertexDistance = fog_spherical_distance(position);
    cylindricalVertexDistance = fog_cylindrical_distance(position);
    vertexColor = modelMarker ? vec4(1.0, 1.0, 1.0, Color.a) : Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    if (!modelMarker && !shadowMarker && !duplicateHatMarker) {
        applyGifEffect();
    }
}
