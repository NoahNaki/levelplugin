package me.nakilex.levelplugin.debug.particles;

import me.nakilex.levelplugin.particles.particles.parents.Particle;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ParticleDebugRegistry {
    private static final String PARTICLE_PACKAGE = "me.nakilex.levelplugin.particles.particles";
    private static final String PARTICLE_PATH = "me/nakilex/levelplugin/particles/particles";
    private final JavaPlugin plugin;
    private List<ParticleDefinition> cached;

    public ParticleDebugRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public List<ParticleDefinition> getParticles() {
        if (cached == null) {
            cached = buildDefinitions();
        }
        return cached;
    }

    private List<ParticleDefinition> buildDefinitions() {
        List<ParticleDefinition> definitions = new ArrayList<>();
        for (Class<? extends Particle> clazz : findParticleClasses()) {
            Constructor<? extends Particle> constructor = findDefaultConstructor(clazz);
            if (constructor == null) {
                continue;
            }
            String displayName = prettify(clazz.getSimpleName());
            definitions.add(new ParticleDefinition(clazz.getName(), displayName, () -> newInstance(constructor)));
        }
        definitions.sort(Comparator.comparing(ParticleDefinition::displayName));
        return definitions;
    }

    private List<Class<? extends Particle>> findParticleClasses() {
        Set<String> classNames = new HashSet<>();
        classNames.addAll(listFromJar());
        classNames.addAll(listFromClassPath());

        List<Class<? extends Particle>> classes = new ArrayList<>();
        ClassLoader loader = plugin.getClass().getClassLoader();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, loader);
                if (!Particle.class.isAssignableFrom(clazz)) {
                    continue;
                }
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Particle> particleClass = (Class<? extends Particle>) clazz;
                classes.add(particleClass);
            } catch (ClassNotFoundException ignored) {
                // Ignore missing classes to keep registry resilient.
            }
        }
        return classes;
    }

    private Set<String> listFromJar() {
        Set<String> classNames = new HashSet<>();
        File file = plugin.getFile();
        if (file == null || !file.isFile()) {
            return classNames;
        }
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(PARTICLE_PATH) || !name.endsWith(".class")) {
                    continue;
                }
                if (name.contains("$") || name.contains("/parents/")) {
                    continue;
                }
                String className = name.replace('/', '.').replace(".class", "");
                if (className.startsWith(PARTICLE_PACKAGE + ".")) {
                    classNames.add(className);
                }
            }
        } catch (IOException ignored) {
            // Ignore to allow fallback to classpath scanning.
        }
        return classNames;
    }

    private Set<String> listFromClassPath() {
        Set<String> classNames = new HashSet<>();
        ClassLoader loader = plugin.getClass().getClassLoader();
        try {
            Enumeration<URL> resources = loader.getResources(PARTICLE_PATH);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("jar".equals(protocol)) {
                    classNames.addAll(listFromJarResource(url));
                } else if ("file".equals(protocol)) {
                    classNames.addAll(listFromDirectory(url));
                }
            }
        } catch (IOException ignored) {
            // Ignore if classpath scanning fails.
        }
        return classNames;
    }

    private Set<String> listFromJarResource(URL url) {
        Set<String> classNames = new HashSet<>();
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(PARTICLE_PATH) || !name.endsWith(".class")) {
                        continue;
                    }
                    if (name.contains("$") || name.contains("/parents/")) {
                        continue;
                    }
                    classNames.add(name.replace('/', '.').replace(".class", ""));
                }
            }
        } catch (IOException ignored) {
            // Ignore if jar parsing fails.
        }
        return classNames;
    }

    private Set<String> listFromDirectory(URL url) {
        Set<String> classNames = new HashSet<>();
        try {
            Path dir = Path.of(url.toURI());
            if (!Files.isDirectory(dir)) {
                return classNames;
            }
            try (var stream = Files.list(dir)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .filter(path -> !path.getFileName().toString().contains("$"))
                        .forEach(path -> {
                            String name = path.getFileName().toString().replace(".class", "");
                            classNames.add(PARTICLE_PACKAGE + "." + name);
                        });
            }
        } catch (IOException | URISyntaxException ignored) {
            // Ignore if filesystem scanning fails.
        }
        return classNames;
    }

    private Constructor<? extends Particle> findDefaultConstructor(Class<? extends Particle> clazz) {
        try {
            Constructor<? extends Particle> constructor = clazz.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Particle newInstance(Constructor<? extends Particle> constructor) {
        try {
            return constructor.newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String prettify(String className) {
        String base = className.startsWith("Particle") ? className.substring("Particle".length()) : className;
        if (base.isBlank()) {
            return "Particle";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(base.charAt(i - 1))) {
                builder.append(' ');
            }
            builder.append(c);
        }
        return builder.toString().trim();
    }

    public record ParticleDefinition(String id, String displayName, ParticleSupplier supplier) {
        public Particle createParticle() {
            return supplier.get();
        }
    }

    @FunctionalInterface
    public interface ParticleSupplier {
        Particle get();
    }
}
