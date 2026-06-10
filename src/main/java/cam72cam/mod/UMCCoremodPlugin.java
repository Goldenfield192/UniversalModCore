package cam72cam.mod;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class UMCCoremodPlugin implements IFMLLoadingPlugin {
    public UMCCoremodPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.feat.universalmodcore.json");
        Mixins.addConfiguration("mixins.fix.universalmodcore.json");

        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            String urlStr = location.toString();

            if (urlStr.startsWith("jar:")) {
                int separatorIndex = urlStr.indexOf("!/");
                if (separatorIndex != -1) {
                    String jarUrlStr = urlStr.substring("jar:".length(), separatorIndex);
                    try {
                        location = new URL(jarUrlStr);
                    } catch (MalformedURLException ignored) {}
                }
            }

            try {
                File file = new File(location.toURI());
                if (file.isFile() && !CoreModManager.getReparseableCoremods().contains(file.getName())) {
                    //Due to FML's bad behavior on processing FMLCorePluginContainsFMLMod we add here manually
                    CoreModManager.getIgnoredMods().remove(file.getName());
                    if (!ModCore.isDevelopmentEnvironment()) {
                        CoreModManager.getReparseableCoremods().add(file.getName());
                    }
                }
            } catch (URISyntaxException e) {
                FMLLog.log.warn(e);
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"cam72cam.mod.loader.ProxyTransformer"};
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

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
