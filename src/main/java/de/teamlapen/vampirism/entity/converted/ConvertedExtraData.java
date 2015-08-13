package de.teamlapen.vampirism.entity.converted;

import net.minecraft.entity.EntityCreature;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by Max on 13.08.2015.
 */
public class ConvertedExtraData<T extends EntityCreature> {
    public int getTexture_id() {
        return texture_id;
    }

    public void setTexture_id(int texture_id) {
        this.texture_id = texture_id;
    }

    private int texture_id;

    public ConvertedExtraData(int texture_id) {
        this.texture_id = texture_id;
    }

    public ConvertedExtraData() {

    }

    public void writeToNbt(NBTTagCompound nbt) {
        nbt.setInteger("texture_id", texture_id);
    }

    public void readFromNbt(NBTTagCompound nbt) {
        texture_id = nbt.getInteger("texture_id");
    }
}
