package de.teamlapen.vampirism.client.render;

import de.teamlapen.vampirism.client.model.ModelBloodAltar4;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar4;
import de.teamlapen.vampirism.tileEntity.TileEntityBloodAltar4.PHASE;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Temp placeholder renderer, will be replaced later
 * 
 * @author Max
 *
 */
@SideOnly(Side.CLIENT)
public class RendererBloodAltar4 extends VampirismTileEntitySpecialRenderer {

	public static final String textureLoc = REFERENCE.MODID + ":textures/blocks/bloodAltar4.png";
	private final ModelBloodAltar4 model;
	private final ResourceLocation texture;
	private final ResourceLocation enderDragonCrystalBeamTextures = new ResourceLocation("textures/entity/endercrystal/endercrystal_beam.png");
	private final ResourceLocation beaconBeamTexture = new ResourceLocation("textures/entity/beacon_beam.png");

	public RendererBloodAltar4() {
		model = new ModelBloodAltar4();
		texture = new ResourceLocation(textureLoc);
	}


	/**
	 * Renders a beam in the world, similar to the dragon healing beam
	 * 
	 * @param relX
	 *            startX relative to the player
	 * @param relY
	 * @param relZ
	 * @param centerX
	 *            startX in world
	 * @param centerY
	 * @param centerZ
	 * @param targetX
	 *            targetX in world
	 * @param targetY
	 * @param targetZ
	 * @param tickStuff
	 *            used to move the beam, use p5 of {@link #renderTileEntity(TileEntity, double, double, double, float,int)} for that
	 * @param beacon
	 *            whether it should be a beacon or a dragon style beam
	 */
	private void renderBeam(double relX, double relY, double relZ, double centerX, double centerY, double centerZ, double targetX, double targetY, double targetZ, float tickStuff, boolean beacon) {
		float f2 = 50000;
		float f3 = MathHelper.sin(f2 * 0.2F) / 2.0F + 0.5F;
		f3 = (f3 * f3 + f3) * 0.2F;
		float wayX = (float) (targetX - centerX);
		float wayY = (float) (targetY - centerY);
		float wayZ = (float) (targetZ - centerZ);
		float distFlat = MathHelper.sqrt_float(wayX * wayX + wayZ * wayZ);
		float dist = MathHelper.sqrt_float(wayX * wayX + wayY * wayY + wayZ * wayZ);
		GL11.glPushMatrix();
		GL11.glTranslatef((float) relX, (float) relY, (float) relZ);
		GL11.glRotatef((float) (-Math.atan2(wayZ, wayX)) * 180.0F / (float) Math.PI - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef((float) (-Math.atan2(distFlat, wayY)) * 180.0F / (float) Math.PI - 90.0F, 1.0F, 0.0F, 0.0F);
		Tessellator tessellator = Tessellator.getInstance();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_CULL_FACE);
		if (beacon) {
			this.bindTexture(beaconBeamTexture);
		} else {
			this.bindTexture(enderDragonCrystalBeamTextures);
		}
		GL11.glColor3d(1.0F, 0.0F, 0.0F);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		float f9 = -(tickStuff * 0.005F);
		float f10 = MathHelper.sqrt_float(wayX * wayX + wayY * wayY + wayZ * wayZ) / 32.0F + f9;
		WorldRenderer worldRenderer=tessellator.getWorldRenderer();
		worldRenderer.startDrawing(5);
		// Add all 2*8 vertex/corners
		byte b0 = 8;
		for (int i = 0; i <= b0; ++i) {
			float f11 = 0.2F * (MathHelper.sin(i % b0 * (float) Math.PI * 2.0F / b0) * 0.75F);
			float f12 = 0.2F * (MathHelper.cos(i % b0 * (float) Math.PI * 2.0F / b0) * 0.75F);
			float f13 = i % b0 * 1.0F / b0;
			worldRenderer.setColorOpaque(255, 0, 0);
			worldRenderer.addVertexWithUV((f11), (f12), 0.0D, f13, f10);
			if (!beacon) {
				worldRenderer.setColorOpaque_I(16777215);
			}

			worldRenderer.addVertexWithUV(f11, f12, dist, f13, f9);
		}

		tessellator.draw();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_FLAT);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
	}


	@Override
	public void renderTileEntity(TileEntity te, double x, double y, double z, float p5, int p6) {
		// Render the altar itself
		TileEntityBloodAltar4 te4 = (TileEntityBloodAltar4) te;
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
		bindTexture(texture);
		GL11.glPushMatrix();
			adjustRotatePivotViaMeta(te);

		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		model.render(null, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
		GL11.glPopMatrix();
		GL11.glPopMatrix();

		// Render the beams if the ritual is running
		PHASE phase = te4==null?PHASE.NOT_RUNNING:te4.getPhase();
		if (phase == PHASE.BEAM1 || phase == PHASE.BEAM2) {
			x += 0.5;
			y += 3;
			z += 0.5;
			// Calculate center coordinates
			double cX = te.getPos().getX() + 0.5;
			double cY = te.getPos().getY() + 3;
			double cZ = te.getPos().getZ() + 0.5;
			try {
				BlockPos[] tips = te4.getTips();
				for (int i = 0; i < tips.length; i++) {
					this.renderBeam(x, y, z, cX, cY, cZ, tips[i].getX() + 0.5, tips[i].getY() + 0.5, tips[i].getZ() + 0.5, te4.getRunningTick() + p5, false);
				}
				if (phase == PHASE.BEAM2) {
					EntityPlayer p = te4.getPlayer();
					if (p != null) {
						double rX = 0, rZ = 0;
						double rY = -0.3;
						double playerY = p.posY;
						/**
						 * Work around for other players seeing the ritual
						 */
						if (!p.equals(Minecraft.getMinecraft().thePlayer)) {
							Entity e = Minecraft.getMinecraft().thePlayer;
							rX += p.posX - e.posX;
							rY += p.posY - e.posY + 1.5D;
							rZ += p.posZ - e.posZ;
							playerY += 1.5D;
						}
						this.renderBeam(rX, rY, rZ, p.posX, playerY, p.posZ, cX, cY + 0.2, cZ, -(te4.getRunningTick() + p5), true);
					}
				}
			} catch (NullPointerException e) {
			}

		}
	}
}
