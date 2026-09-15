#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 playerCanvas;
flat in int playerModelType;
flat in int playerModelState;
flat in float playerModelPhase;
flat in int playerModelAnimation;

out vec4 fragColor;

mat3 rotateX(float angle) {
    float c = cos(angle), s = sin(angle);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotateY(float angle) {
    float c = cos(angle), s = sin(angle);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

mat3 rotateZ(float angle) {
    float c = cos(angle), s = sin(angle);
    return mat3(c, s, 0.0, -s, c, 0.0, 0.0, 0.0, 1.0);
}

bool hitBox(vec3 rayOrigin, vec3 rayDirection, vec3 halfSize, out float distance, out vec3 point, out vec3 normal) {
    vec3 inverseDirection = vec3(
        abs(rayDirection.x) < 0.00001 ? 100000.0 : 1.0 / rayDirection.x,
        abs(rayDirection.y) < 0.00001 ? 100000.0 : 1.0 / rayDirection.y,
        abs(rayDirection.z) < 0.00001 ? 100000.0 : 1.0 / rayDirection.z
    );
    vec3 first = (-halfSize - rayOrigin) * inverseDirection;
    vec3 second = (halfSize - rayOrigin) * inverseDirection;
    vec3 nearPlane = min(first, second);
    vec3 farPlane = max(first, second);
    float nearDistance = max(max(nearPlane.x, nearPlane.y), nearPlane.z);
    float farDistance = min(min(farPlane.x, farPlane.y), farPlane.z);
    if (farDistance < max(nearDistance, 0.0)) return false;

    distance = nearDistance > 0.0 ? nearDistance : farDistance;
    point = rayOrigin + rayDirection * distance;
    vec3 edge = abs(abs(point) - halfSize);
    if (edge.x <= edge.y && edge.x <= edge.z) normal = vec3(sign(point.x), 0.0, 0.0);
    else if (edge.y <= edge.z) normal = vec3(0.0, sign(point.y), 0.0);
    else normal = vec3(0.0, 0.0, sign(point.z));
    return true;
}

void considerPart(
    int part,
    vec3 center,
    vec3 halfSize,
    mat3 rotation,
    vec3 rayOrigin,
    vec3 rayDirection,
    inout float bestDistance,
    inout int bestPart,
    inout vec3 bestPoint,
    inout vec3 bestNormal,
    inout vec3 bestWorldNormal
) {
    mat3 inverseRotation = transpose(rotation);
    vec3 localOrigin = inverseRotation * (rayOrigin - center);
    vec3 localDirection = inverseRotation * rayDirection;
    float distance;
    vec3 point;
    vec3 normal;
    if (hitBox(localOrigin, localDirection, halfSize, distance, point, normal) && distance < bestDistance) {
        bestDistance = distance;
        bestPart = part;
        bestPoint = point;
        bestNormal = normal;
        bestWorldNormal = rotation * normal;
    }
}

vec2 partOrigin(int part, bool overlay) {
    if (part == 0) return overlay ? vec2(32.0, 0.0) : vec2(0.0, 0.0);
    if (part == 1) return overlay ? vec2(16.0, 32.0) : vec2(16.0, 16.0);
    if (part == 2) return overlay ? vec2(40.0, 32.0) : vec2(40.0, 16.0);
    if (part == 3) return overlay ? vec2(48.0, 48.0) : vec2(32.0, 48.0);
    if (part == 4) return overlay ? vec2(0.0, 32.0) : vec2(0.0, 16.0);
    return overlay ? vec2(0.0, 48.0) : vec2(16.0, 48.0);
}

bool slimSkin() {
    // Slim skins leave the two columns removed from the classic right arm transparent.
    return texelFetch(Sampler0, ivec2(54, 20), 0).a < 0.1
        && texelFetch(Sampler0, ivec2(54, 23), 0).a < 0.1;
}

vec3 partDimensions(int part) {
    if (part == 0) return vec3(8.0, 8.0, 8.0);
    if (part == 1) return vec3(8.0, 12.0, 4.0);
    if (part == 2 || part == 3) return vec3(slimSkin() ? 3.0 : 4.0, 12.0, 4.0);
    return vec3(4.0, 12.0, 4.0);
}

vec2 skinCoordinate(int part, vec3 point, vec3 normal, bool overlay) {
    vec3 dimensions = partDimensions(part);
    vec3 halfSize = dimensions * 0.5;
    float width = dimensions.x;
    float height = dimensions.y;
    float depth = dimensions.z;
    vec2 origin = partOrigin(part, overlay);
    vec2 localUv;
    vec2 rectOrigin;
    vec2 rectSize;

    if (normal.z > 0.5) {
        localUv = vec2((point.x + halfSize.x) / width, (halfSize.y - point.y) / height);
        rectOrigin = origin + vec2(depth, depth);
        rectSize = vec2(width, height);
    } else if (normal.z < -0.5) {
        localUv = vec2((halfSize.x - point.x) / width, (halfSize.y - point.y) / height);
        rectOrigin = origin + vec2(depth + width + depth, depth);
        rectSize = vec2(width, height);
    } else if (normal.x < -0.5) {
        localUv = vec2((point.z + halfSize.z) / depth, (halfSize.y - point.y) / height);
        rectOrigin = origin + vec2(0.0, depth);
        rectSize = vec2(depth, height);
    } else if (normal.x > 0.5) {
        localUv = vec2((halfSize.z - point.z) / depth, (halfSize.y - point.y) / height);
        rectOrigin = origin + vec2(depth + width, depth);
        rectSize = vec2(depth, height);
    } else if (normal.y > 0.5) {
        localUv = vec2((point.x + halfSize.x) / width, (point.z + halfSize.z) / depth);
        rectOrigin = origin + vec2(depth, 0.0);
        rectSize = vec2(width, depth);
    } else {
        localUv = vec2((point.x + halfSize.x) / width, (halfSize.z - point.z) / depth);
        rectOrigin = origin + vec2(depth + width, 0.0);
        rectSize = vec2(width, depth);
    }
    return (rectOrigin + clamp(localUv, 0.001, 0.999) * rectSize) / 64.0;
}

vec4 samplePart(int part, vec3 point, vec3 normal) {
    vec4 base = texture(Sampler0, skinCoordinate(part, point, normal, false));
    vec4 overlay = texture(Sampler0, skinCoordinate(part, point, normal, true));
    vec3 rgb = mix(base.rgb, overlay.rgb, overlay.a);
    return vec4(rgb, max(base.a, overlay.a));
}

bool tracePlayerModel(out vec4 modelColor) {
    float height = playerModelType == 0 ? 10.0 : (playerModelType == 1 ? 22.0 : 34.0);
    vec3 rayOrigin = vec3((playerCanvas.x - 0.5) * 18.0, (0.5 - playerCanvas.y) * height, 40.0);
    vec3 rayDirection = vec3(0.0, 0.0, -1.0);
    float phase = playerModelPhase;
    float turn = radians(30.0 + 6.0 * sin(phase * 0.7));
    float headTurn = radians(12.0 * sin(phase * 0.53));
    float headPitch = radians(8.0 * sin(phase * 0.41));
    float rightArmPitch = 0.05 * sin(phase * 1.1);
    float leftArmPitch = -rightArmPitch;
    float rightArmRoll = 0.0;
    float leftArmRoll = 0.0;
    float rightLegPitch = 0.0;
    float leftLegPitch = 0.0;
    float bodyPitch = 0.0;
    float verticalShift = 0.0;
    float breath = 0.12 * sin(playerModelPhase * 0.9);

    if (playerModelAnimation == 1) { // wave
        turn = radians(24.0 + 3.0 * sin(phase * 0.5));
        rightArmPitch = radians(165.0 + 10.0 * sin(phase * 2.2));
        rightArmRoll = radians(-12.0 + 10.0 * sin(phase * 2.2));
        leftArmPitch = 0.04 * sin(phase);
        headTurn = radians(-8.0 + 5.0 * sin(phase * 0.7));
    } else if (playerModelAnimation == 2) { // walk
        float stride = 0.65 * sin(phase * 1.4);
        rightArmPitch = stride;
        leftArmPitch = -stride;
        rightLegPitch = -stride;
        leftLegPitch = stride;
        verticalShift = 0.18 * abs(sin(phase * 1.4));
    } else if (playerModelAnimation == 3) { // run
        float stride = 1.05 * sin(phase * 2.1);
        rightArmPitch = stride;
        leftArmPitch = -stride;
        rightLegPitch = -stride * 0.9;
        leftLegPitch = stride * 0.9;
        bodyPitch = radians(10.0);
        verticalShift = 0.45 * abs(sin(phase * 2.1));
    } else if (playerModelAnimation == 4) { // attack / punch
        rightArmPitch = radians(-78.0) + 0.22 * sin(phase * 2.4);
        leftArmPitch = radians(12.0) + 0.08 * sin(phase);
        bodyPitch = radians(5.0);
        headTurn = radians(-10.0);
    } else if (playerModelAnimation == 5) { // crouch
        bodyPitch = radians(18.0 + 2.0 * sin(phase * 0.8));
        verticalShift = -3.0;
        rightArmPitch = radians(-18.0) + 0.04 * sin(phase);
        leftArmPitch = radians(-18.0) - 0.04 * sin(phase);
        rightLegPitch = radians(18.0);
        leftLegPitch = radians(18.0);
        headPitch = radians(-10.0);
    }

    float bestDistance = 100000.0;
    int bestPart = -1;
    vec3 bestPoint = vec3(0.0);
    vec3 bestNormal = vec3(0.0);
    vec3 bestWorldNormal = vec3(0.0);

    if (playerModelType == 0) {
        mat3 headRotation = rotateY(turn + headTurn) * rotateX(headPitch);
        considerPart(0, vec3(0.0), vec3(4.0), headRotation, rayOrigin, rayDirection,
            bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
    } else {
        float torsoY = (playerModelType == 1 ? -4.0 : 2.0) + verticalShift;
        float headY = torsoY + 10.0 + breath;
        float armWidth = slimSkin() ? 3.0 : 4.0;
        float armX = 4.0 + armWidth * 0.5;
        mat3 bodyRotation = rotateY(turn) * rotateX(bodyPitch);
        mat3 headRotation = rotateY(turn + headTurn) * rotateX(headPitch);
        mat3 rightArmRotation = bodyRotation * rotateZ(rightArmRoll) * rotateX(rightArmPitch);
        mat3 leftArmRotation = bodyRotation * rotateZ(leftArmRoll) * rotateX(leftArmPitch);
        vec3 rightArmCenter = vec3(-armX, torsoY + 6.0, 0.0)
            + rightArmRotation * vec3(0.0, -6.0, 0.0);
        vec3 leftArmCenter = vec3(armX, torsoY + 6.0, 0.0)
            + leftArmRotation * vec3(0.0, -6.0, 0.0);
        considerPart(0, vec3(0.0, headY, 0.0), vec3(4.0), headRotation, rayOrigin, rayDirection,
            bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
        considerPart(1, vec3(0.0, torsoY + breath, 0.0), vec3(4.0, 6.0, 2.0), bodyRotation, rayOrigin, rayDirection,
            bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
        considerPart(2, rightArmCenter, vec3(armWidth * 0.5, 6.0, 2.0), rightArmRotation, rayOrigin, rayDirection,
            bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
        considerPart(3, leftArmCenter, vec3(armWidth * 0.5, 6.0, 2.0), leftArmRotation, rayOrigin, rayDirection,
            bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
        if (playerModelType == 2) {
            mat3 rightLegRotation = rotateY(turn) * rotateX(rightLegPitch);
            mat3 leftLegRotation = rotateY(turn) * rotateX(leftLegPitch);
            vec3 rightLegCenter = vec3(-2.0, -4.0, 0.0) + rightLegRotation * vec3(0.0, -6.0, 0.0);
            vec3 leftLegCenter = vec3(2.0, -4.0, 0.0) + leftLegRotation * vec3(0.0, -6.0, 0.0);
            considerPart(4, rightLegCenter, vec3(2.0, 6.0, 2.0), rightLegRotation, rayOrigin, rayDirection,
                bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
            considerPart(5, leftLegCenter, vec3(2.0, 6.0, 2.0), leftLegRotation, rayOrigin, rayDirection,
                bestDistance, bestPart, bestPoint, bestNormal, bestWorldNormal);
        }
    }

    if (bestPart < 0) return false;
    vec4 skin = samplePart(bestPart, bestPoint, bestNormal);
    vec3 lightDirection = normalize(vec3(-0.35, 0.45, 1.0));
    float light = clamp(0.68 + 0.32 * dot(normalize(bestWorldNormal), lightDirection), 0.48, 1.0);
    modelColor = vec4(skin.rgb * light, skin.a * vertexColor.a);
    return modelColor.a >= 0.1;
}

void main() {
    if (playerModelState < 0) discard;

    vec4 color;
    if (playerModelState > 0) {
        if (!tracePlayerModel(color)) discard;
        color *= ColorModulator;
    } else {
        color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
        if (color.a < 0.1) discard;
    }

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
