package de.teamlapen.vampirism.client.render.converted;

import de.teamlapen.vampirism.client.render.TextureHelper;
import de.teamlapen.vampirism.entity.EntityConvertedCreature;
import de.teamlapen.vampirism.entity.converted.BiteableRegistry;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelCow;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.util.ResourceLocation;

/**
 * Created by Max on 13.08.2015.
 */
public class RenderConvertibleDefault extends RenderLiving implements IRenderConvertible {

    protected final ResourceLocation[] locations;
    protected final ResourceLocation[] oldLocations;
    protected final ResourceLocation overlay;

    public RenderConvertibleDefault(ModelBase model, float shadow, ResourceLocation overlay, ResourceLocation... textures) {
        super(model, shadow);
        locations = new ResourceLocation[textures.length];
        oldLocations = textures;
        this.overlay = overlay;
        for (int i = 0; i < textures.length; i++) {
            ResourceLocation res = textures[i];
            locations[i] = new ResourceLocation("vampirism/temp/" + res.getResourceDomain() + "/" + res.getResourcePath());
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return getEntityTexture((EntityConvertedCreature) entity);
    }

    @Override
    public ResourceLocation getEntityTexture(EntityConvertedCreature entity) {
        int i = (entity).getTextureId();
        if (i < 0 || i >= locations.length) {
            i = 0;
        }
        TextureHelper.createVampireTexture(overlay, oldLocations[i], locations[i]);
        return locations[i];

    }

    @Override
    public void doRender(EntityConvertedCreature entity, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        super.doRender(entity, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }

    public static void registerVanillaRenderes() {
        BiteableRegistry.registerRenderer(EntityCow.class, new RenderConvertibleDefault(new ModelCow(), 0.7F, getVanillaOverlay("cow"), new ResourceLocation("textures/entity/cow/cow.png")));
    }

    /**
     * Makes a resource location out of the given name, but does not guarantee that it exists
     *
     * @param s
     * @return
     */
    public static ResourceLocation getVanillaOverlay(String s) {
        return new ResourceLocation(REFERENCE.MODID + ":textures/entity/vanilla/" + s + "Overlay.png");
    }
}
