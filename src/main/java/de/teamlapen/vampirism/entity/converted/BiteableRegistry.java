package de.teamlapen.vampirism.entity.converted;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import de.teamlapen.vampirism.Configs;
import de.teamlapen.vampirism.client.render.converted.IRenderConvertible;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityCreature;
import org.eclipse.jdt.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Max on 13.08.2015.
 */
public class BiteableRegistry {

    /**
     * Stores the convertibles classes during initialization
     */
    private static final Map<Class<? extends EntityCreature>, IConvertibleAnalyzer> convertableMap = new HashMap<Class<? extends EntityCreature>, IConvertibleAnalyzer>();
    /**
     * Stores the renderer class assignment during initialization
     */
    @SideOnly(Side.CLIENT)
    private static final Map<Class<? extends EntityCreature>, IRenderConvertible> convertableRenderMap = new HashMap<Class<? extends EntityCreature>, IRenderConvertible>();

    /**
     * Stores the bitable entries after initialization
     */
    private static final Map<String, BiteableEntry> biteables = new HashMap<String, BiteableEntry>();
    private static final String TAG = "BiteableRegistry";
    private static boolean finished = false;

    public static void registerConvertable(Class<? extends EntityCreature> clazz, IConvertibleAnalyzer analyzer) {
        if (analyzer != null && !analyzer.getConvertibleClass().isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The biteable entry has to represent the class to register or a superclass of it");
        }
        convertableMap.put(clazz, analyzer);
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderer(Class<? extends EntityCreature> clazz, IRenderConvertible render) {
        convertableRenderMap.put(clazz, render);
    }

    public static void finishRegistration() {
        if (finished) return;
        HashMap<String, Integer> bloodValues = Configs.bloodValues;
        float bloodValueMultiplier = 1;
        Integer i = bloodValues.get("multiplier");
        if (i != null) {
            bloodValueMultiplier = i / 10F;
        }

        for (Map.Entry<Class<? extends EntityCreature>, IConvertibleAnalyzer> entry : convertableMap.entrySet()) {
            Integer blood = bloodValues.remove(entry.getKey().getName());
            if (blood == null) {
                Logger.w(TAG, "Missing blood value for convertable creature %s", entry.getKey().getName());
                continue;
            }
            Logger.d(TAG, "Registering %s with %d blood as convertible", entry.getKey().getName(), blood);
            BiteableEntry biteable = new BiteableEntry(entry.getKey(), (int) (blood * bloodValueMultiplier), true);
            biteable.setAnalyzer(entry.getValue());

            if (FMLCommonHandler.instance().getSide().isClient()) {
                IRenderConvertible render = convertableRenderMap.get(entry.getKey());
                if (render == null) {
                    throw new IllegalStateException("Cannot finish convertible registration. The renderer for " + entry.getKey() + " is missing");
                }
                biteable.setRenderer(render);
            }
            biteables.put(entry.getKey().getName(), biteable);
        }
        for (Map.Entry<String, Integer> entry : bloodValues.entrySet()) {
            biteables.put(entry.getKey(), new BiteableEntry(EntityCreature.class, (int) (Math.abs(entry.getValue()) * bloodValueMultiplier), false));
        }
        bloodValues.clear();
        convertableMap.clear();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            convertableRenderMap.clear();
        }

        finished = true;
    }

    public static BiteableEntry getEntry(Class<? extends EntityCreature> clazz) {
        return getEntry(clazz.getName());
    }

    public static
    @NonNull
    BiteableEntry getEntry(String clazz) {
        BiteableEntry entry = biteables.get(clazz);
        return entry == null ? BiteableEntry.defaultEntry : entry;
    }

    @SideOnly(Side.CLIENT)
    public static void setRenderManager(RenderManager manager) {
        for (BiteableEntry entry : biteables.values()) {
            if (entry.convertable) {
                entry.getRenderer().setRenderManager(manager);
            }
        }
    }
}
