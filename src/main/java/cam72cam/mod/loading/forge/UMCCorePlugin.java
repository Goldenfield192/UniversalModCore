package cam72cam.mod.loading.forge;

import cam72cam.mod.ModCore;
import cam72cam.mod.loading.UMCMod;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ModContainerFactory;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.Type;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class UMCCorePlugin implements IFMLLoadingPlugin {
    public UMCCorePlugin() {
        //Bootstrap mixin 0.8.2
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.feat.universalmodcore.json");
        Mixins.addConfiguration("mixins.fix.universalmodcore.json");

        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                File file = new File(location.toURI());
                if (file.isFile() && !CoreModManager.getReparseableCoremods().contains(file.getName())) {
                    //Due to FML's bad behaviour on processing FMLCorePluginContainsFMLMod we add here manually
                    CoreModManager.getIgnoredMods().remove(file.getName());
                    if (!ModCore.isDevelopmentEnvironment()) {
                        CoreModManager.getReparseableCoremods().add(file.getName());
                    }
                }
            } catch (URISyntaxException e) {
                FMLLog.log.warn(e);
            }
        }

        ModContainerFactory.instance().registerContainerType(Type.getType(UMCMod.class), UMCModContainer.class);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
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
