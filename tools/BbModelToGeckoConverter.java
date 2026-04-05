import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BbModelToGeckoConverter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double EPSILON = 1.0e-4;

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: BbModelToGeckoConverter <bbmodel> <geoOut> <animationOut> <textureOut>");
            System.exit(1);
        }

        Path bbmodelPath = Path.of(args[0]);
        Path geoOut = Path.of(args[1]);
        Path animationOut = Path.of(args[2]);
        Path textureOut = Path.of(args[3]);

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(bbmodelPath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonArray groups = root.getAsJsonArray("groups");
        JsonArray elements = root.getAsJsonArray("elements");
        JsonArray outliner = root.getAsJsonArray("outliner");

        Map<String, JsonObject> groupByUuid = mapByUuid(groups);
        Map<String, JsonObject> elementByUuid = mapByUuid(elements);

        List<JsonObject> bones = new ArrayList<>();
        for (JsonElement entry : outliner) {
            processOutlinerNode(entry, null, groupByUuid, elementByUuid, bones);
        }

        writeGeo(root, bones, geoOut);
        writeAnimations(root, animationOut);
        writeTexture(root, textureOut);
    }

    private static Map<String, JsonObject> mapByUuid(JsonArray array) {
        Map<String, JsonObject> map = new HashMap<>();
        if (array == null) {
            return map;
        }

        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            map.put(object.get("uuid").getAsString(), object);
        }

        return map;
    }

    private static void processOutlinerNode(
            JsonElement entry,
            String parentName,
            Map<String, JsonObject> groupByUuid,
            Map<String, JsonObject> elementByUuid,
            List<JsonObject> bones
    ) {
        JsonObject outlinerNode;
        String uuid;
        if (entry.isJsonPrimitive()) {
            uuid = entry.getAsString();
            JsonObject group = groupByUuid.get(uuid);
            if (group == null) {
                return;
            }
            outlinerNode = buildMinimalOutlinerEntry(group);
        } else {
            outlinerNode = entry.getAsJsonObject();
            uuid = outlinerNode.get("uuid").getAsString();
        }

        JsonObject group = groupByUuid.get(uuid);
        if (group == null) {
            return;
        }

        String name = group.get("name").getAsString();
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parentName != null) {
            bone.addProperty("parent", parentName);
        }

        JsonArray pivot = cloneArray(group.getAsJsonArray("origin"));
        bone.add("pivot", pivot);

        JsonArray rotation = cloneArray(group.getAsJsonArray("rotation"));
        if (!isZeroVector(rotation)) {
            bone.add("rotation", rotation);
        }

        if (group.has("reset") && group.get("reset").getAsBoolean()) {
            bone.addProperty("reset", true);
        }

        if (group.has("visibility") && !group.get("visibility").getAsBoolean()) {
            bone.addProperty("neverRender", true);
        }

        JsonArray cubes = new JsonArray();
        JsonArray children = outlinerNode.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                String childUuid;
                JsonObject childNode = null;
                if (child.isJsonPrimitive()) {
                    childUuid = child.getAsString();
                } else {
                    childNode = child.getAsJsonObject();
                    childUuid = childNode.get("uuid").getAsString();
                }

                JsonObject element = elementByUuid.get(childUuid);
                if (element != null) {
                    JsonObject cube = convertElementToCube(element);
                    if (cube != null) {
                        cubes.add(cube);
                    }
                    continue;
                }

                if (groupByUuid.containsKey(childUuid)) {
                    JsonElement nestedEntry = childNode != null ? childNode : buildMinimalOutlinerEntry(groupByUuid.get(childUuid));
                    processOutlinerNode(nestedEntry, name, groupByUuid, elementByUuid, bones);
                }
            }
        }

        if (!cubes.isEmpty()) {
            bone.add("cubes", cubes);
        }

        bones.add(bone);
    }

    private static JsonObject convertElementToCube(JsonObject element) {
        if (!element.has("vertices") || !element.has("faces")) {
            return null;
        }

        Map<String, double[]> vertices = parseVertices(element.getAsJsonObject("vertices"));
        if (vertices.size() != 8) {
            return null;
        }

        Bounds bounds = computeBounds(vertices);
        JsonObject cube = new JsonObject();
        cube.add("origin", toJsonArray(bounds.minX, bounds.minY, bounds.minZ));
        cube.add("size", toJsonArray(bounds.maxX - bounds.minX, bounds.maxY - bounds.minY, bounds.maxZ - bounds.minZ));
        cube.add("pivot", cloneArray(element.getAsJsonArray("origin")));

        JsonArray rotation = cloneArray(element.getAsJsonArray("rotation"));
        if (!isZeroVector(rotation)) {
            cube.add("rotation", rotation);
        }

        JsonObject uvFaces = new JsonObject();
        JsonObject faces = element.getAsJsonObject("faces");
        for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
            JsonObject face = faceEntry.getValue().getAsJsonObject();
            JsonArray faceVertices = face.getAsJsonArray("vertices");
            JsonObject faceUv = face.getAsJsonObject("uv");
            String direction = detectDirection(faceVertices, vertices, bounds);
            if (direction == null) {
                continue;
            }

            UvBounds uv = computeUvBounds(faceVertices, faceUv);
            JsonObject uvObject = new JsonObject();
            uvObject.add("uv", toJsonArray(uv.minU, uv.minV));
            uvObject.add("uv_size", toJsonArray(uv.maxU - uv.minU, uv.maxV - uv.minV));
            uvFaces.add(direction, uvObject);
        }

        if (uvFaces.size() == 0) {
            return null;
        }

        cube.add("uv", uvFaces);
        return cube;
    }

    private static JsonObject buildMinimalOutlinerEntry(JsonObject group) {
        JsonObject entry = new JsonObject();
        entry.addProperty("uuid", group.get("uuid").getAsString());
        JsonArray children = new JsonArray();
        JsonArray groupChildren = group.getAsJsonArray("children");
        if (groupChildren != null) {
            for (JsonElement child : groupChildren) {
                if (child.isJsonPrimitive()) {
                    children.add(child.getAsString());
                }
            }
        }
        entry.add("children", children);
        return entry;
    }

    private static Map<String, double[]> parseVertices(JsonObject verticesObject) {
        Map<String, double[]> vertices = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : verticesObject.entrySet()) {
            JsonArray coordinates = entry.getValue().getAsJsonArray();
            vertices.put(entry.getKey(), new double[] {
                    coordinates.get(0).getAsDouble(),
                    coordinates.get(1).getAsDouble(),
                    coordinates.get(2).getAsDouble()
            });
        }
        return vertices;
    }

    private static Bounds computeBounds(Map<String, double[]> vertices) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double[] vertex : vertices.values()) {
            minX = Math.min(minX, vertex[0]);
            minY = Math.min(minY, vertex[1]);
            minZ = Math.min(minZ, vertex[2]);
            maxX = Math.max(maxX, vertex[0]);
            maxY = Math.max(maxY, vertex[1]);
            maxZ = Math.max(maxZ, vertex[2]);
        }

        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static String detectDirection(JsonArray faceVertices, Map<String, double[]> vertices, Bounds bounds) {
        double firstX = vertices.get(faceVertices.get(0).getAsString())[0];
        double firstY = vertices.get(faceVertices.get(0).getAsString())[1];
        double firstZ = vertices.get(faceVertices.get(0).getAsString())[2];

        boolean sameX = true;
        boolean sameY = true;
        boolean sameZ = true;

        for (JsonElement vertexIdElement : faceVertices) {
            double[] vertex = vertices.get(vertexIdElement.getAsString());
            sameX &= approx(vertex[0], firstX);
            sameY &= approx(vertex[1], firstY);
            sameZ &= approx(vertex[2], firstZ);
        }

        if (sameX) {
            return approx(firstX, bounds.maxX) ? "east" : "west";
        }

        if (sameY) {
            return approx(firstY, bounds.maxY) ? "up" : "down";
        }

        if (sameZ) {
            return approx(firstZ, bounds.maxZ) ? "south" : "north";
        }

        return null;
    }

    private static UvBounds computeUvBounds(JsonArray faceVertices, JsonObject faceUv) {
        double minU = Double.POSITIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;

        for (JsonElement vertexIdElement : faceVertices) {
            JsonArray uv = faceUv.getAsJsonArray(vertexIdElement.getAsString());
            double u = uv.get(0).getAsDouble();
            double v = uv.get(1).getAsDouble();
            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);
        }

        return new UvBounds(minU, minV, maxU, maxV);
    }

    private static void writeGeo(JsonObject sourceRoot, List<JsonObject> bones, Path output) throws IOException {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.soul_boss");
        JsonObject resolution = sourceRoot.getAsJsonObject("resolution");
        description.addProperty("texture_width", resolution.get("width").getAsDouble());
        description.addProperty("texture_height", resolution.get("height").getAsDouble());
        description.addProperty("visible_bounds_width", 4.0);
        description.addProperty("visible_bounds_height", 5.0);
        description.add("visible_bounds_offset", toJsonArray(0.0, 2.0, 0.0));

        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        JsonArray bonesArray = new JsonArray();
        bones.stream()
                .sorted(Comparator.comparing(b -> b.get("name").getAsString()))
                .forEach(bonesArray::add);
        geometry.add("bones", bonesArray);

        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.12.0");
        JsonArray geometryArray = new JsonArray();
        geometryArray.add(geometry);
        root.add("minecraft:geometry", geometryArray);

        writeJson(output, root);
    }

    private static void writeAnimations(JsonObject sourceRoot, Path output) throws IOException {
        JsonObject animationsObject = new JsonObject();
        JsonArray animations = sourceRoot.getAsJsonArray("animations");
        for (JsonElement animationElement : animations) {
            JsonObject animation = animationElement.getAsJsonObject();
            String name = animation.get("name").getAsString();
            double length = animation.get("length").getAsDouble();
            if (length <= 0.0) {
                continue;
            }

            JsonObject animationOut = new JsonObject();
            animationOut.addProperty("animation_length", length);
            animationOut.addProperty("loop", convertLoop(animation.get("loop").getAsString()));

            JsonObject bonesObject = new JsonObject();
            JsonObject animators = animation.getAsJsonObject("animators");
            for (Map.Entry<String, JsonElement> animatorEntry : animators.entrySet()) {
                JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                JsonArray keyframes = animator.getAsJsonArray("keyframes");
                if (keyframes == null || keyframes.isEmpty()) {
                    continue;
                }

                JsonObject boneChannels = new JsonObject();
                Map<String, List<JsonObject>> byChannel = new HashMap<>();
                for (JsonElement keyframeElement : keyframes) {
                    JsonObject keyframe = keyframeElement.getAsJsonObject();
                    byChannel.computeIfAbsent(keyframe.get("channel").getAsString(), ignored -> new ArrayList<>()).add(keyframe);
                }

                for (Map.Entry<String, List<JsonObject>> channelEntry : byChannel.entrySet()) {
                    JsonObject channelObject = new JsonObject();
                    channelEntry.getValue().stream()
                            .sorted(Comparator.comparingDouble(k -> k.get("time").getAsDouble()))
                            .forEach(keyframe -> channelObject.add(
                                    formatTime(keyframe.get("time").getAsDouble()),
                                    extractVector(keyframe)
                            ));
                    boneChannels.add(channelEntry.getKey(), channelObject);
                }

                if (boneChannels.size() > 0) {
                    bonesObject.add(animator.get("name").getAsString(), boneChannels);
                }
            }

            animationOut.add("bones", bonesObject);
            animationsObject.add(name, animationOut);
        }

        JsonObject root = new JsonObject();
        root.add("animations", animationsObject);
        writeJson(output, root);
    }

    private static void writeTexture(JsonObject sourceRoot, Path output) throws IOException {
        JsonArray textures = sourceRoot.getAsJsonArray("textures");
        JsonObject bestTexture = null;
        int bestArea = -1;
        for (JsonElement textureElement : textures) {
            JsonObject texture = textureElement.getAsJsonObject();
            int width = texture.get("width").getAsInt();
            int height = texture.get("height").getAsInt();
            int area = width * height;
            if (area > bestArea && texture.has("source")) {
                bestArea = area;
                bestTexture = texture;
            }
        }

        if (bestTexture == null) {
            throw new IllegalStateException("No embedded texture found in source model");
        }

        String source = bestTexture.get("source").getAsString();
        String prefix = "data:image/png;base64,";
        if (!source.startsWith(prefix)) {
            throw new IllegalStateException("Unexpected texture source format");
        }

        byte[] textureBytes = Base64.getDecoder().decode(source.substring(prefix.length()));
        Files.createDirectories(output.getParent());
        Files.write(output, textureBytes);
    }

    private static JsonArray extractVector(JsonObject keyframe) {
        JsonArray dataPoints = keyframe.getAsJsonArray("data_points");
        JsonObject point = dataPoints.get(0).getAsJsonObject();
        return toJsonArray(
                point.get("x").getAsDouble(),
                point.get("y").getAsDouble(),
                point.get("z").getAsDouble()
        );
    }

    private static String convertLoop(String loop) {
        return switch (loop.toLowerCase(Locale.ROOT)) {
            case "loop" -> "loop";
            case "hold" -> "hold_on_last_frame";
            default -> "play_once";
        };
    }

    private static String formatTime(double time) {
        return String.format(Locale.ROOT, "%.5f", time).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void writeJson(Path output, JsonObject object) throws IOException {
        Files.createDirectories(output.getParent());
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            GSON.toJson(object, writer);
        }
    }

    private static JsonArray cloneArray(JsonArray source) {
        JsonArray clone = new JsonArray();
        if (source == null) {
            clone.add(0.0);
            clone.add(0.0);
            clone.add(0.0);
            return clone;
        }

        for (JsonElement element : source) {
            clone.add(element.getAsDouble());
        }

        return clone;
    }

    private static JsonArray toJsonArray(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
    }

    private static boolean isZeroVector(JsonArray array) {
        if (array == null || array.size() < 3) {
            return true;
        }
        return approx(array.get(0).getAsDouble(), 0.0)
                && approx(array.get(1).getAsDouble(), 0.0)
                && approx(array.get(2).getAsDouble(), 0.0);
    }

    private static boolean approx(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    private record UvBounds(double minU, double minV, double maxU, double maxV) {}
}
