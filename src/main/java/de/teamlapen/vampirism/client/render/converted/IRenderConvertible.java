package de.teamlapen.vampirism.client.render.converted;

import de.teamlapen.vampirism.entity.EntityConvertedCreature;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/**
 * Created by Max on 13.08.2015.
 */
public interface IRenderConvertible {

    ResourceLocation getEntityTexture(EntityConvertedCreature entity);

    void doRender(EntityConvertedCreature entity, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_);

    void setRenderManager(RenderManager manager);
}
