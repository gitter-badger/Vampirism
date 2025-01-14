package de.teamlapen.vampirism.entity.convertible;

import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 15.08.2015.
 */
public class EntityConvertedSheep extends EntityConvertedCreature implements IShearable {


    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return !getSheared();
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        setSheared(true);
        int i = 1 + rand.nextInt(3);
        for (int j = 0; j < i; j++) {
            ret.add(new ItemStack(Blocks.wool, 1, getFleeceColor().getMetadata()));
        }
        this.playSound("mob.sheep.shear", 1.0F, 1.0F);
        return ret;
    }

    public static class ConvertingSheepHandler extends ConvertingHandler<EntitySheep> {
        @Override
        public EntityConvertedCreature createFrom(EntitySheep entity) {
            EntityConvertedSheep creature = new EntityConvertedSheep(entity.worldObj);
            this.copyImportantStuff(creature, entity);
            creature.setSheared(entity.getSheared());
            return creature;
        }
    }

    private Boolean lastSheared = null;

    public EntityConvertedSheep(World world) {
        super(world);
    }


    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        boolean t = getSheared();
        if (!nil() && (lastSheared == null || lastSheared.booleanValue() != t)) {
            lastSheared = t;
            ((EntitySheep) getEntityCreature()).setSheared(lastSheared);

        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, new Byte((byte) 0));
    }

    /**
     * returns true if a sheeps wool has been sheared
     */
    public boolean getSheared() {
        return (this.dataWatcher.getWatchableObjectByte(16) & 16) != 0;
    }

    public void setSheared(boolean p_70893_1_) {
        byte b0 = this.dataWatcher.getWatchableObjectByte(16);

        if (p_70893_1_) {
            this.dataWatcher.updateObject(16, Byte.valueOf((byte) (b0 | 16)));
        } else {
            this.dataWatcher.updateObject(16, Byte.valueOf((byte) (b0 & -17)));
        }
    }



    public EnumDyeColor getFleeceColor() {
        return nil() ? EnumDyeColor.BLACK : ((EntitySheep) this.getEntityCreature()).getFleeceColor();//this.dataWatcher.getWatchableObjectByte(16) & 15;
    }


    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("Sheared", this.getSheared());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.setSheared(nbt.getBoolean("Sheared"));
    }
}
