package de.teamlapen.vampirism.client.render;

import de.teamlapen.vampirism.entity.EntityConvertedCreature;
import de.teamlapen.vampirism.entity.converted.BiteableEntry;
import de.teamlapen.vampirism.entity.converted.BiteableRegistry;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Created by Max on 13.08.2015.
 */
public class RendererConvertedCreature extends Render {
    @Override
    public void doRender(Entity p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        doRender((EntityConvertedCreature) p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }


    public void doRender(EntityConvertedCreature convertedCreature, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        BiteableEntry entry = convertedCreature.getBiteableEntry();
        if (entry == null) {
            Logger.w("RenderConverted", "Cannot render converted creature %s without biteable entry", convertedCreature);
        } else {
            entry.getRenderer().doRender(convertedCreature, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
        }
    }

    /**
     * This should never be called
     *
     * @param p_110775_1_
     * @return
     */
    @Override
    @Deprecated
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return getEntityTexture(p_110775_1_);
    }


    @Override
    public void setRenderManager(RenderManager p_76976_1_) {
        super.setRenderManager(p_76976_1_);
        BiteableRegistry.setRenderManager(p_76976_1_);
    }

    /**
     * This should never be called
     *
     * @param convertedCreature
     * @return
     */
    protected ResourceLocation getEntityTexture(EntityConvertedCreature convertedCreature) {
        BiteableEntry entry = convertedCreature.getBiteableEntry();
        if (entry == null) {
            Logger.w("RenderConverted", "Cannot get converted texture %s without biteable entry", convertedCreature);
            return new ResourceLocation("empty");
        } else {
            return entry.getRenderer().getEntityTexture(convertedCreature);
        }
    }
}
