package cam72cam.mod;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.FMLCorePlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class UMCMixinPlugin implements IFMLLoadingPlugin {
    public static List<File> UMCMods;
    public static List<String> UMC_classes;

    public UMCMixinPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.universalmodcore.json");

        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                File file = new File(location.toURI());
                if (file.isFile()) {
                    CoreModManager.getIgnoredMods().remove(file.getName());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        File modsDir = new File((File) data.get("mcLocation"), "umc");
        List<File> validJars;

        UMCMods = new ArrayList<>();
        File[] array = modsDir.listFiles();
        if (array != null) {
            UMCMods.addAll(Arrays.stream(array)
                                 .filter(file -> file.isFile() && file.getName().endsWith(".jar"))
                                 .filter(jar -> {
                                     try {
                                         ZipFile zipFile = new ZipFile(jar);
                                         return zipFile.getEntry("universalmodcore.json") != null;
                                     } catch (IOException e) {
                                         throw new RuntimeException(e);
                                     }
                                 })
                                 .collect(Collectors.toList()));
        }

        if (!UMCMods.isEmpty()) {
            try {
                ClassLoader loader = getClass().getClassLoader();
                if (loader instanceof LaunchClassLoader) {
                    for (File f : UMCMods) {
                        ((LaunchClassLoader) loader).addURL(f.toURI().toURL());
                        UMC_classes = findSubclasses(f.getAbsolutePath(), ModCore.Mod.class);
                        System.out.println(UMC_classes);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static List<String> findSubclasses(String jarPath, Class<?> targetClass)
            throws IOException, ClassNotFoundException {
        List<String> result = new ArrayList<>();
        JarFile jarFile = new JarFile(new File(jarPath));

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                String className = entry.getName()
                                        .replace("/", ".")
                                        .replace(".class", "");

                try {
                    try {
                        JarEntry e = jarFile.getJarEntry(className.replace('.', '/') + ".class");
                        if (e == null) {
                            throw new ClassNotFoundException(className);
                        }
                        InputStream inputStream = jarFile.getInputStream(e);
                        byte[] classBytes = new byte[inputStream.available()];
                        inputStream.read(classBytes);
                        ClassReader reader = new ClassReader(classBytes);
                        if(reader.getSuperName().replace("/", ".").equals(targetClass.getName())) {
                            result.add(className);
                        }
                    } catch (IOException e) {
                        throw new ClassNotFoundException("Failed to load class: " + className, e);
                    }
                } catch (Throwable t) {
                    // 忽略无法加载的类
                    System.err.println("Warning: Failed to load class " + className +
                                               ": " + t.getMessage());
                }
            }
        }

        jarFile.close();
        return result;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
