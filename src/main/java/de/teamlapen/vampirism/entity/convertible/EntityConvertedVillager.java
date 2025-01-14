package de.teamlapen.vampirism.entity.convertible;

import de.teamlapen.vampirism.ModItems;
import de.teamlapen.vampirism.entity.ai.VVillagerAILookAtCustomer;
import de.teamlapen.vampirism.entity.ai.VVillagerAITrade;
import de.teamlapen.vampirism.entity.ai.VampireAIMoveIndoors;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.item.ItemBloodBottle;
import de.teamlapen.vampirism.util.Helper18;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Entity for converted villagers. Able to trade with player
 */
public class EntityConvertedVillager extends EntityConvertedCreature implements IMerchant {


    private boolean isWillingToTrade;

    public static class VillagerConvertingHandler extends ConvertingHandler<EntityVillager> {
        @Override
        public EntityConvertedCreature createFrom(EntityVillager entity) {
            EntityConvertedCreature convertedCreature = new EntityConvertedVillager(entity.worldObj);
            this.copyImportantStuff(convertedCreature, entity);
            return convertedCreature;
        }

        @Override
        public double getConvertedSpeed(EntityVillager entity) {
            return 0.295D;
        }

        @Override
        public void dropConvertedItems(EntityVillager entity, boolean recentlyHit, int looting) {
            entity.dropItem(ModItems.weakVampireFang, 1);
        }
    }

    private EntityPlayer buyingPlayer;
    private MerchantRecipeList buyingList;
    private int timeUntilReset;
    private boolean needsInitilization;

    public EntityConvertedVillager(World world) {
        super(world);
        Helper18.setBreakDoors(this, true);
        this.tasks.addTask(1, new VVillagerAILookAtCustomer(this));
        this.tasks.addTask(1, new VVillagerAITrade(this));
        this.tasks.addTask(2, new VampireAIMoveIndoors(this));
        this.tasks.addTask(4, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(10, new EntityAIMoveThroughVillage(this, 0.6, false));

    }

    /**
     * Returns the sound this mob makes while it's alive.
     */
    protected String getLivingSound() {
        return this.isTrading() ? "mob.villager.haggle" : "mob.villager.idle";
    }

    /**
     * Returns the sound this mob makes when it is hurt.
     */
    protected String getHurtSound() {
        return "mob.villager.hit";
    }

    /**
     * Returns the sound this mob makes on death.
     */
    protected String getDeathSound() {
        return "mob.villager.death";
    }


    public boolean isTrading() {
        return buyingPlayer != null;
    }

    @Override
    public void setCustomer(EntityPlayer player) {
        buyingPlayer = player;
    }

    @Override
    public EntityPlayer getCustomer() {
        return buyingPlayer;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (!this.isTrading() && this.timeUntilReset > 0) {
            --this.timeUntilReset;

            if (this.timeUntilReset <= 0) {
                if (this.needsInitilization) {
                    if (this.buyingList.size() > 1) {
                        Iterator iterator = this.buyingList.iterator();

                        while (iterator.hasNext()) {
                            MerchantRecipe merchantrecipe = (MerchantRecipe) iterator.next();

                            if (merchantrecipe.isRecipeDisabled()) {
                                merchantrecipe.increaseMaxTradeUses(this.rand.nextInt(6) + this.rand.nextInt(6) + 2);
                            }
                        }
                    }

                    this.addDefaultEquipmentAndRecipies();
                    this.needsInitilization = false;
                }

                this.addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 0));
            }
        }
    }

    @Override
    public MerchantRecipeList getRecipes(EntityPlayer p_70934_1_) {
        if (this.buyingList == null) {
            this.addDefaultEquipmentAndRecipies();
        }

        return this.buyingList;
    }

    private void addDefaultEquipmentAndRecipies() {
        MerchantRecipeList merchantRecipeList = new MerchantRecipeList();

        addRecipe(merchantRecipeList, new ItemStack(ModItems.humanHeart, 9), 2, this.getRNG(), 0.5F);
        addRecipe(merchantRecipeList, 3, new ItemStack(ModItems.humanHeart, 9), this.getRNG(), 0.5F);
        addRecipe(merchantRecipeList, 1, new ItemStack(ModItems.leechSword, 1), this.getRNG(), 0.3F);
        addRecipe(merchantRecipeList, 1, new ItemStack(ModItems.bloodBottle, 3, ItemBloodBottle.MAX_BLOOD), rand, 0.9F);

        Collections.shuffle(merchantRecipeList);

        if (this.buyingList == null) {
            this.buyingList = new MerchantRecipeList();
        }
        int i=rand.nextInt(2)+1;
        for (int l = 0; l <i&& l < merchantRecipeList.size(); ++l) {
            this.buyingList.add(merchantRecipeList.remove(rand.nextInt(merchantRecipeList.size())));
        }
    }

    @Override
    public void setRecipes(MerchantRecipeList p_70930_1_) {
    }

    public static void addRecipe(MerchantRecipeList list, ItemStack item, int emeralds, Random rnd, float prop) {
        if (rnd.nextFloat() < prop) {
            list.add(new MerchantRecipe(item, new ItemStack(Items.emerald, emeralds)));
        }
    }

    public static void addRecipe(MerchantRecipeList list, int emeralds, ItemStack item, Random rnd, float prop) {
        if (rnd.nextFloat() < prop) {
            list.add(new MerchantRecipe(new ItemStack(Items.emerald, emeralds), item));
        }
    }

    @Override
    public void useRecipe(MerchantRecipe recipe) {
        recipe.incrementToolUses();
        this.livingSoundTime = -this.getTalkInterval();
        this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());

        int xp = 3 + this.rand.nextInt(4);

        if(recipe.getToolUses()==1||this.rand.nextInt(5)==0){
            this.timeUntilReset=40;
            this.needsInitilization=true;
            this.isWillingToTrade=true;
            xp+=5;
        }

        if(recipe.getRewardsExp()){
            this.worldObj.spawnEntityInWorld(new EntityXPOrb(this.worldObj, this.posX, this.posY + 0.5D, this.posZ, xp));
        }

    }

    @Override
    public void verifySellingItem(ItemStack p_110297_1_) {
        if (!this.worldObj.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
            this.livingSoundTime = -this.getTalkInterval();

            if (p_110297_1_ != null) {
                this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());
            } else {
                this.playSound("mob.villager.no", this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    public boolean interact(EntityPlayer player) {
        ItemStack itemstack = player.inventory.getCurrentItem();
        boolean flag = itemstack != null && itemstack.getItem() == Items.spawn_egg;

        if (!flag && this.isEntityAlive() && !this.isTrading() && !this.isChild() && !player.isSneaking() && VampirePlayer.get(player).getLevel() > 0) {
            if (!this.worldObj.isRemote) {
                this.setCustomer(player);
                player.displayVillagerTradeGui(this);
            }

            return true;
        } else {
            return super.interact(player);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("Offers", 10)) {
            NBTTagCompound list = nbt.getCompoundTag("Offers");
            this.buyingList = new MerchantRecipeList(list);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);

        if (this.buyingList != null) {
            nbt.setTag("Offers", this.buyingList.getRecipiesAsTags());
        }
    }
}
