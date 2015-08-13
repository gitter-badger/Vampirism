package de.teamlapen.vampirism.entity;

import de.teamlapen.vampirism.entity.converted.BiteableEntry;
import de.teamlapen.vampirism.entity.converted.BiteableRegistry;
import de.teamlapen.vampirism.entity.converted.ConvertedExtraData;
import de.teamlapen.vampirism.network.ISyncable;
import de.teamlapen.vampirism.util.Logger;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Created by Max on 13.08.2015.
 */
public class EntityConvertedCreature extends EntityVampireBase implements ISyncable {

    private static final String TAG = "ConvertedCreature";
    private ConvertedExtraData data;
    private String entity_class = "";
    private BiteableEntry biteableEntry;

    public EntityConvertedCreature(World world) {
        super(world);
    }

    public static EntityConvertedCreature createFrom(EntityCreature creature) {
        BiteableEntry entry = BiteableRegistry.getEntry(creature.getClass());
        if (!entry.convertable) {
            Logger.w(TAG, "Cannot convert %s since there is no convertable entry for %s", creature, creature.getClass().getName());
            return null;
        }
        EntityConvertedCreature converted = new EntityConvertedCreature(creature.worldObj);
        converted.entity_class = creature.getClass().getName();
        converted.copyLocationAndAnglesFrom(creature);
        converted.biteableEntry = entry;
        converted.data = entry.getData(creature);
        return converted;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        data.writeToNbt(nbt);
        nbt.setString("entity_class", entity_class);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        entity_class = nbt.getString("entity_class");
        biteableEntry = BiteableRegistry.getEntry(entity_class);
        if (!biteableEntry.convertable) {
            Logger.w(TAG, "Cannot find convertable entry for %s (%s) -> Deleting entity", entity_class, this);
            this.setDead();
        } else {
            data = biteableEntry.createEmptyData();
            data.readFromNbt(nbt);
        }
    }

    public BiteableEntry getBiteableEntry() {
        return biteableEntry;
    }

    public int getTextureId() {
        return data == null ? 0 : data.getTexture_id();
    }

    public ConvertedExtraData getExtraData() {
        return data;
    }

    @Override
    public void loadUpdateFromNBT(NBTTagCompound nbt) {
        if (entity_class.isEmpty() || biteableEntry == null) {
            entity_class = nbt.getString("entity_class");
            biteableEntry = BiteableRegistry.getEntry(entity_class);
            if (!biteableEntry.convertable) {
                Logger.w(TAG, "Cannot find convertable entry for %s (%s) -> Deleting entity", entity_class, this);
                this.setDead();
                return;
            } else {
                data = biteableEntry.createEmptyData();
            }
        }
        data.readFromNbt(nbt);
    }

    @Override
    public void writeFullUpdateToNBT(NBTTagCompound nbt) {
        data.writeToNbt(nbt);
        nbt.setString("entity_class", entity_class);
    }

    @Override
    public String getCommandSenderName() {
        return (String) EntityList.classToStringMapping.get(biteableEntry.clazz);
    }

    @Override
    public String toString() {
        return "[" + super.toString() + " representing " + entity_class + "]";
    }
}
