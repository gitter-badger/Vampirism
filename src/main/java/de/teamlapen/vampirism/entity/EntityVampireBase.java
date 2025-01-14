package de.teamlapen.vampirism.entity;

import de.teamlapen.vampirism.ModPotion;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.util.Helper;
import de.teamlapen.vampirism.util.Helper18;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * Base class for all vampires
 */
public abstract class EntityVampireBase extends EntityVampirism {
    private boolean sundamageCache;
    protected float sundamage = 0.5f;

    public EntityVampireBase(World p_i1595_1_) {
        super(p_i1595_1_);
        Helper18.setAvoidsWater(this, true);
        this.tasks.addTask(0, new EntityAISwimming(this));
    }

    public boolean isGettingSundamage() {
        return isGettingSundamage(false);
    }

    public boolean isGettingSundamage(boolean forceRefresh) {
        if (this.ticksExisted % 5 != 0) return sundamageCache;
        float brightness = this.getBrightness(1.0F);
        boolean canSeeSky = this.worldObj.canBlockSeeSky(this.getPosition());
        if (brightness > 0.5F) {
            if (Helper.isEntityInVampireBiome(this)) return false;
            if (VampirismMod.isSunDamageTime(this.worldObj) && canSeeSky) {
                sundamageCache = true;
                return true;
            }
        }
        sundamageCache = false;
        return false;
    }

    protected void attackedEntityAsMob(EntityLivingBase entity) {
        if (this.rand.nextInt(3) == 0) {
            (entity).addPotionEffect(new PotionEffect(Potion.weakness.id, 200));
            (entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 100));
        }

    }

    protected boolean isValidLightLevel() {
        if (Helper.isEntityInVampireBiome(this)) return true;
        BlockPos pos=new BlockPos(this.posX,this.getEntityBoundingBox().minY,this.posZ);


        if (this.worldObj.getLightFor(EnumSkyBlock.SKY, pos) > this.rand.nextInt(32)) {
            return false;
        } else {
            int l = this.worldObj.getLightFromNeighbors(pos);

            if (this.worldObj.isThundering()) {
                int i1 = this.worldObj.getSkylightSubtracted();
                this.worldObj.setSkylightSubtracted(10);
                l = this.worldObj.getLightFromNeighbors(pos);
                this.worldObj.setSkylightSubtracted(i1);
            }

            return l <= this.rand.nextInt(8);
        }
    }

    public boolean wantsBlood() {
        return false;
    }

    public void addBlood(int amt) {
        addPotionEffect(new PotionEffect(Potion.regeneration.id, amt * 20));
    }

    @Override
    public boolean getCanSpawnHere() {
        if (!isValidLightLevel()) return false;
        return super.getCanSpawnHere();
    }

    @Override
    public void onLivingUpdate() {
        if (!this.worldObj.isRemote) {
            if (isGettingSundamage()) {
                float dmg = sundamage;
                if (this.isPotionActive(ModPotion.sunscreen)) {
                    dmg = dmg / 2;
                }
                this.attackEntityFrom(VampirismMod.sunDamage, dmg);
            }
        }
        super.onLivingUpdate();
    }
}
