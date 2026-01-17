package me.nakilex.levelplugin.utils;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class GlowPacketUtil {
    private static final byte GLOWING_FLAG = 0x40;
    private static final int ENTITY_FLAGS_INDEX = 0;

    private GlowPacketUtil() {
    }

    public static void applyGlowing(PacketContainer packet, boolean glowing) {
        if (!applyDataValues(packet, glowing)) {
            applyWatchables(packet, glowing);
        }
    }

    private static boolean applyDataValues(PacketContainer packet, boolean glowing) {
        try {
            Method method = packet.getClass().getMethod("getDataValueCollectionModifier");
            StructureModifier<?> modifier = (StructureModifier<?>) method.invoke(packet);
            Object rawValues = modifier.read(0);
            if (!(rawValues instanceof List<?> list)) {
                return false;
            }
            List<Object> values = new ArrayList<>();
            values.addAll((List<?>) list);

            boolean updated = false;
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                Integer index = getIndex(value);
                Object current = getValue(value);
                if (index != null && index == ENTITY_FLAGS_INDEX && current instanceof Byte) {
                    byte flags = (byte) current;
                    byte next = glowing ? (byte) (flags | GLOWING_FLAG) : (byte) (flags & ~GLOWING_FLAG);
                    Object serializer = getSerializer(value);
                    Object replacement = newWrappedDataValue(ENTITY_FLAGS_INDEX, serializer, next);
                    if (replacement == null) {
                        return false;
                    }
                    values.set(i, replacement);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                Object serializer = getDefaultByteSerializer();
                Object wrapped = newWrappedDataValue(
                    ENTITY_FLAGS_INDEX,
                    serializer,
                    glowing ? GLOWING_FLAG : (byte) 0
                );

                if (wrapped == null) {
                    return false;
                }
                values.add(wrapped);
            }
            @SuppressWarnings("unchecked")
            StructureModifier<List<?>> listMod = (StructureModifier<List<?>>) modifier;
            listMod.write(0, (List<?>) values);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static Object getDefaultByteSerializer() {
        try {
            // ProtocolLib 5.x internal registry
            Class<?> registry = Class.forName("com.comphenix.protocol.wrappers.WrappedDataWatcher$Registry");
            Method get = registry.getMethod("get", Class.class);
            return get.invoke(null, Byte.class);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to resolve Byte serializer", ex);
        }
    }


    private static void applyWatchables(PacketContainer packet, boolean glowing) {
        List<WrappedWatchableObject> watchables = new ArrayList<>(packet.getWatchableCollectionModifier().read(0));
        boolean found = false;
        for (int i = 0; i < watchables.size(); i++) {
            WrappedWatchableObject val = watchables.get(i);
            if (val.getIndex() == ENTITY_FLAGS_INDEX && val.getValue() instanceof Byte) {
                byte flags = (byte) val.getValue();
                byte next = glowing ? (byte) (flags | GLOWING_FLAG) : (byte) (flags & ~GLOWING_FLAG);
                watchables.set(i, new WrappedWatchableObject(ENTITY_FLAGS_INDEX, next));
                found = true;
                break;
            }
        }
        if (!found) {
            watchables.add(new WrappedWatchableObject(ENTITY_FLAGS_INDEX, glowing ? GLOWING_FLAG : (byte) 0));
        }
        packet.getWatchableCollectionModifier().write(0, watchables);
    }

    private static Integer getIndex(Object value) throws ReflectiveOperationException {
        Method method = value.getClass().getMethod("getIndex");
        Object result = method.invoke(value);
        return result instanceof Integer index ? index : null;
    }

    private static Object getValue(Object value) throws ReflectiveOperationException {
        Method method = value.getClass().getMethod("getValue");
        return method.invoke(value);
    }

    private static Object getSerializer(Object value) throws ReflectiveOperationException {
        Method method = value.getClass().getMethod("getSerializer");
        return method.invoke(value);
    }

    private static Object newWrappedDataValue(int index, Object serializer, Object value) {
        try {
            Class<?> wrappedDataValue = Class.forName("com.comphenix.protocol.wrappers.WrappedDataValue");
            Constructor<?> ctor = wrappedDataValue.getConstructor(int.class, WrappedDataWatcher.Serializer.class, Object.class);
            return ctor.newInstance(index, serializer, value);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
