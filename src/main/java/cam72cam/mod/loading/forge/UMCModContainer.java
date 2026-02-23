package cam72cam.mod.loading.forge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.FormattedMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class UMCModContainer extends DummyModContainer {
    private final String modID;

    private final String displayName;
    private final String version;
    private final URL updateURL;

    public UMCModContainer(String className, ModCandidate container, Map<String, Object> modDescriptor) {
        this.modID = (String) modDescriptor.get("modid");

        try (JarFile file = new JarFile(container.getModContainer())) {
            ZipEntry modInfo = file.getEntry("umc.json");
            try (InputStreamReader stream = new InputStreamReader(file.getInputStream(modInfo))) {
                JsonObject umcMod = new JsonParser().parse(stream).getAsJsonObject();

                this.displayName = umcMod.get("name").getAsString();
                this.version = umcMod.get("version").getAsString();
                String update = umcMod.get("updateURL").getAsString();
                this.updateURL = update != null ? new URL(update) : null;

            }
        } catch (IOException ignored) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getModId() {
        return modID;
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public String getDisplayVersion() {
        return version;
    }

    @Override
    public URL getUpdateUrl() {
        return updateURL;
    }

    @Override
    public boolean matches(Object mod)
    {
        return mod == modInstance;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void constructMod(FMLConstructionEvent event)
    {
        ModClassLoader modClassLoader = event.getModClassLoader();
        try
        {
            modClassLoader.addFile(source);
        }
        catch (MalformedURLException e)
        {
            FormattedMessage message = new FormattedMessage("{} Failed to add file to classloader: {}", getModId(), source);
            throw new LoaderException(message.getFormattedMessage(), e);
        }
        modClassLoader.clearNegativeCacheFor(candidate.getClassList());

        //Only place I could think to add this...
        MinecraftForge.preloadCrashClasses(event.getASMHarvestedData(), getModId(), candidate.getClassList());

        Class<?> clazz;
        try
        {
            clazz = Class.forName(className, true, modClassLoader);
        }
        catch (ClassNotFoundException e)
        {
            FormattedMessage message = new FormattedMessage("{} Failed load class: {}", getModId(), className);
            throw new LoaderException(message.getFormattedMessage(), e);
        }

        Certificate[] certificates = clazz.getProtectionDomain().getCodeSource().getCertificates();
        ImmutableList<String> certList = CertificateHelper.getFingerprints(certificates);
        sourceFingerprints = ImmutableSet.copyOf(certList);

        String expectedFingerprint = (String)descriptor.get("certificateFingerprint");

        fingerprintNotPresent = true;

        if (expectedFingerprint != null && !expectedFingerprint.isEmpty())
        {
            if (!sourceFingerprints.contains(expectedFingerprint))
            {
                Level warnLevel = source.isDirectory() ? Level.TRACE : Level.ERROR;
                FMLLog.log.log(warnLevel, "The mod {} is expecting signature {} for source {}, however there is no signature matching that description", getModId(), expectedFingerprint, source.getName());
            }
            else
            {
                certificate = certificates[certList.indexOf(expectedFingerprint)];
                fingerprintNotPresent = false;
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> props = (List<Map<String, String>>)descriptor.get("customProperties");
        if (props != null)
        {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            for (Map<String, String> p : props)
            {
                builder.put(p.get("k"), p.get("v"));
            }
            customModProperties = builder.build();
        }
        else
        {
            customModProperties = EMPTY_PROPERTIES;
        }

        Boolean hasDisableableFlag = (Boolean)descriptor.get("canBeDeactivated");
        boolean hasReverseDepends = !event.getReverseDependencies().get(getModId()).isEmpty();
        if (hasDisableableFlag != null && hasDisableableFlag)
        {
            disableability = hasReverseDepends ? Disableable.DEPENDENCIES : Disableable.YES;
        }
        else
        {
            disableability = hasReverseDepends ? Disableable.DEPENDENCIES : Disableable.RESTART;
        }
        Method factoryMethod = gatherAnnotations(clazz);
        ILanguageAdapter languageAdapter = getLanguageAdapter();
        try
        {
            modInstance = languageAdapter.getNewInstance(this, clazz, modClassLoader, factoryMethod);
        }
        catch (Exception e)
        {
            FormattedMessage message = new FormattedMessage("{} Failed to load new mod instance.", getModId());
            throw new LoaderException(message.getFormattedMessage(), e);
        }
        NetworkRegistry.INSTANCE.register(this, clazz, (String)(descriptor.getOrDefault("acceptableRemoteVersions", null)), event.getASMHarvestedData());
        if (fingerprintNotPresent)
        {
            eventBus.post(new FMLFingerprintViolationEvent(source.isDirectory(), source, ImmutableSet.copyOf(this.sourceFingerprints), expectedFingerprint));
        }
        ProxyInjector.inject(this, event.getASMHarvestedData(), FMLCommonHandler.instance().getSide(), languageAdapter);
        AutomaticEventSubscriber.inject(this, event.getASMHarvestedData(), FMLCommonHandler.instance().getSide());
        ConfigManager.sync(this.getModId(), Config.Type.INSTANCE);

        try
        {
            processFieldAnnotations(event.getASMHarvestedData());
        }
        catch (IllegalAccessException e)
        {
            FormattedMessage message = new FormattedMessage("{} Failed to process field annotations.", getModId());
            throw new LoaderException(message.getFormattedMessage(), e);
        }
    }


    @Override
    public String toString() {
        return "UMC Mod:" + modID;
    }
}
