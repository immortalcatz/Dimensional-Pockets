package net.gtn.dimensionalpocket.client.renderer.tile;

import static org.lwjgl.opengl.GL11.*;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Random;

import me.jezza.oc.client.gui.lib.Colour;
import net.gtn.dimensionalpocket.common.core.pocket.Pocket;
import net.gtn.dimensionalpocket.common.core.pocket.PocketSideState;
import net.gtn.dimensionalpocket.common.lib.Reference;
import net.gtn.dimensionalpocket.common.tileentity.TileDimensionalPocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TileRendererPocket extends TileEntitySpecialRenderer {
    
    static EnumMap<PocketSideState, Colour> stateColours = new EnumMap<>(PocketSideState.class);
    static {
        Colour colour = Colour.WHITE.copy();
        colour.a = 100.0 / 255;
        stateColours.put(PocketSideState.NONE, colour);
        
        colour = Colour.GREEN.copy();
        colour.a = 100.0 / 255.0;
        stateColours.put(PocketSideState.ENERGY, colour);
    }
    
    FloatBuffer floatBuffer = GLAllocation.createDirectFloatBuffer(16);

    protected boolean inRange;
    private float stateColorLevel;
    private float fieldTranslation;
    private ItemStack itemStack;
    
    private static final int planeCount = 15;
    
    private static final int maxBrightness = 220;
    private static final int fieldBrightness = maxBrightness;

    private Random random = new Random(31100L);

    protected static ResourceLocation tunnel = new ResourceLocation(Reference.MOD_IDENTIFIER + "textures/misc/tunnel.png");
    protected static ResourceLocation particleField = new ResourceLocation(Reference.MOD_IDENTIFIER + "textures/misc/particleField.png");
    protected static ResourceLocation reducedParticleField = new ResourceLocation(Reference.MOD_IDENTIFIER + "textures/misc/particleField32.png");

    protected static ResourceLocation pocketFrame = new ResourceLocation(Reference.MOD_IDENTIFIER + "textures/blocks/dimensionalPocket.png");
    protected static ResourceLocation basicOverlay = new ResourceLocation(Reference.MOD_IDENTIFIER + "textures/blocks/dimensionalPocket_overlay_none.png");
    
    protected EnumMap<PocketSideState, ResourceLocation> overlays = new EnumMap<>(PocketSideState.class);
    {
        //overlays.put(FlowState.NONE, basicOverlay);
        overlays.put(PocketSideState.ENERGY, basicOverlay);
    }
    
    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float tick) {
        if (tile instanceof TileDimensionalPocket)
            renderDimensionalPocketAt((TileDimensionalPocket) tile, x, y, z, tick, null, null, null);
    }

    @Override
    protected void bindTexture(ResourceLocation texture) {
        if (itemStack != null)
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        else
            super.bindTexture(texture);
    }
    
    protected void updateStateColorLevel() {
        long colorCycleTime = 1337L;
        double minColorLevel = 0.5;
        this.stateColorLevel = (float) (minColorLevel + (1-minColorLevel) * Math.sin((System.currentTimeMillis()%colorCycleTime) * Math.PI / colorCycleTime));
    }
    
    /**
     * Set time in ms it should take the particle field to translate once completely
     * @param cycleTime
     */
    protected void updateFieldTranslation(float speed) {
        long cycleTime = (long) (250000L/speed);
        this.fieldTranslation = System.currentTimeMillis() % cycleTime / ((float) cycleTime);
    }

    /**
     * Method is used by tile and item renderer.
     * Last three arguments are passed by the item renderer.
     * if itemStack is null (and tile is not null) it is rendering a tile,
     * otherwise it is rendering an item
     */
    public void renderDimensionalPocketAt(TileDimensionalPocket tile, double x, double y, double z, float f, ItemStack itemStack, ItemRenderType itemRenderType, Object[] data) {
        this.itemStack = itemStack;
        double maxDistance = 32.0; // distance to block
        this.inRange = (tile == null) || Minecraft.getMinecraft().renderViewEntity.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) < (maxDistance * maxDistance);

        glPushMatrix();
        if (itemStack == null)
            glDisable(GL_FOG);
        else {
            if (itemRenderType == ItemRenderType.INVENTORY)
                glTranslatef(0.0F, -0.1F, 0.0F);
            if (itemRenderType == ItemRenderType.ENTITY)
                glTranslatef(-0.5F, -0.4F, -0.5F);
        }

        updateFieldTranslation(2F);
        // Y Neg
        drawPlane(0, x, y, z, 0.001, 1.0);
        // Y Pos
        drawPlane(1, x, y, z, 0.999, 1.0);
        // Z Neg
        drawPlane(2, x, y, z, 0.001, 1.0);
        // Z Pos
        drawPlane(3, x, y, z, 0.999, 1.0);
        // X Neg
        drawPlane(4, x, y, z, 0.001, 1.0);
        // X Pos
        drawPlane(5, x, y, z, 0.999, 1.0);

        glDisable(GL_LIGHTING);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Tessellator instance = Tessellator.instance;

        instance.setBrightness(maxBrightness);

        renderFaces(x, y, z, 0, null, pocketFrame);

        Pocket pocket = (tile == null) ? null : tile.getPocket();
        
        updateStateColorLevel();
        
        renderFaces(x, y, z, 0.0001d, pocket, null);

        glDisable(GL_BLEND);

        glEnable(GL_LIGHTING);
        if (itemStack == null)
            glEnable(GL_FOG);
        
        glPopMatrix();
    }
    
    /**
     * Prepares the rendering for the given side and returns whether the rendering should proceed or not.
     * @param isOverlay
     * @param side
     * @param pocket
     * @param instance
     * @return
     */
    protected boolean prepareRenderForSide(ResourceLocation texture, ForgeDirection side, Pocket pocket, Tessellator instance) {
        if (texture == null) {
            PocketSideState state = (pocket == null) ? PocketSideState.NONE : pocket.getFlowState(side);
            ResourceLocation overlayTexture = overlays.get(state);
            if (overlayTexture == null)
                return false;
            
            instance.startDrawingQuads();
            bindTexture(overlayTexture);
            instance.setBrightness(maxBrightness);
            Colour c = stateColours.get(state);
            instance.setColorRGBA_F((float) c.r * stateColorLevel,
                                    (float) c.g * stateColorLevel,
                                    (float) c.b * stateColorLevel,
                                    (float) c.a);
        } else {
            instance.startDrawingQuads();
            bindTexture(texture);
            instance.setBrightness(maxBrightness);
            instance.setColorRGBA(255, 255, 255, 255);
        }
        return true;
    }

    private void renderFaces(double x, double y, double z, double offset, Pocket pocket, ResourceLocation texture) {
        Tessellator instance = Tessellator.instance;

        // @formatter:off
		// Y Neg
        if (prepareRenderForSide(texture, ForgeDirection.DOWN, pocket, instance)) {
    		instance.addVertexWithUV(x          , y - offset, z          , 1.0D, 1.0D);
    		instance.addVertexWithUV(x + 1.0D   , y - offset, z          , 1.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D   , y - offset, z + 1.0D   , 0.0D, 0.0D);
    		instance.addVertexWithUV(x          , y - offset, z + 1.0D   , 0.0D, 1.0D);
    		instance.draw();
        }
		
		// Y Pos
        if (prepareRenderForSide(texture, ForgeDirection.UP, pocket, instance)) {
    		instance.addVertexWithUV(x          , y + 1.0D + offset, z + 1.0D, 1.0D, 1.0D);
    		instance.addVertexWithUV(x + 1.0D   , y + 1.0D + offset, z + 1.0D, 1.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D   , y + 1.0D + offset, z       , 0.0D, 0.0D);
    		instance.addVertexWithUV(x          , y + 1.0D + offset, z       , 0.0D, 1.0D);
    		instance.draw();
        }
		
		// Z Neg
        if (prepareRenderForSide(texture, ForgeDirection.NORTH, pocket, instance)) {
    		instance.addVertexWithUV(x          , y + 1.0D  , z - offset, 0.0D, 1.0D);
    		instance.addVertexWithUV(x + 1.0D   , y + 1.0D  , z - offset, 1.0D, 1.0D);
    		instance.addVertexWithUV(x + 1.0D   , y         , z - offset, 1.0D, 0.0D);
    		instance.addVertexWithUV(x          , y         , z - offset, 0.0D, 0.0D);
    		instance.draw();
        }
		
		// Z Pos
        if (prepareRenderForSide(texture, ForgeDirection.SOUTH, pocket, instance)) {
    		instance.addVertexWithUV(x          , y + 1.0D  , z + 1.0D + offset, 1.0D, 1.0D);
    		instance.addVertexWithUV(x          , y         , z + 1.0D + offset, 1.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D   , y         , z + 1.0D + offset, 0.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D   , y + 1.0D  , z + 1.0D + offset, 0.0D, 1.0D);
    		instance.draw();
        }
		
		// X Neg
        if (prepareRenderForSide(texture, ForgeDirection.WEST, pocket, instance)) {
    		instance.addVertexWithUV(x - offset, y       , z         , 1.0D, 0.0D);
    		instance.addVertexWithUV(x - offset, y       , z + 1.0D  , 0.0D, 0.0D);
    		instance.addVertexWithUV(x - offset, y + 1.0D, z + 1.0D  , 0.0D, 1.0D);
    		instance.addVertexWithUV(x - offset, y + 1.0D, z         , 1.0D, 1.0D);
    		instance.draw();
        }
		
		// X Pos
        if (prepareRenderForSide(texture, ForgeDirection.EAST, pocket, instance)) {
    		instance.addVertexWithUV(x + 1.0D + offset, y        , z + 1.0D  , 1.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D + offset, y        , z         , 0.0D, 0.0D);
    		instance.addVertexWithUV(x + 1.0D + offset, y + 1.0D , z         , 0.0D, 1.0D);
    		instance.addVertexWithUV(x + 1.0D + offset, y + 1.0D , z + 1.0D  , 1.0D, 1.0D);
    		instance.draw();
        }
		
		// @formatter:on
    }

    protected void drawPlane(int side, double x, double y, double z, double offset, double scale) {
        float dX = (float) TileEntityRendererDispatcher.staticPlayerX;
        float dY = (float) TileEntityRendererDispatcher.staticPlayerY;
        float dZ = (float) TileEntityRendererDispatcher.staticPlayerZ;

        glPushMatrix();
        glDisable(GL_LIGHTING);
        random.setSeed(31100L);
        if (inRange) {
            switch (side) {
                case 0:
                    drawPlaneYNeg(dX, dY, dZ, x, y, z, offset, scale);
                    break;
                case 1:
                    drawPlaneYPos(dX, dY, dZ, x, y, z, offset, scale);
                    break;
                case 2:
                    drawPlaneZNeg(dX, dY, dZ, x, y, z, offset, scale);
                    break;
                case 3:
                    drawPlaneZPos(dX, dY, dZ, x, y, z, offset, scale);
                    break;
                case 4:
                    drawPlaneXNeg(dX, dY, dZ, x, y, z, offset, scale);
                    break;
                case 5:
                    drawPlaneXPos(dX, dY, dZ, x, y, z, offset, scale);
                    break;
            }
        } else {
            renderOutOfRange(side, x, y, z, offset, scale);
        }

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_GEN_S);
        glDisable(GL_TEXTURE_GEN_T);
        glDisable(GL_TEXTURE_GEN_R);
        glDisable(GL_TEXTURE_GEN_Q);
        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

    public void renderOutOfRange(int side, double x, double y, double z, double offset, double scale) {
        glPushMatrix();
        bindTexture(reducedParticleField);
        Tessellator instance = Tessellator.instance;
        instance.startDrawingQuads();
        instance.setBrightness(fieldBrightness);
        instance.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // @formatter:off
		switch (side) {
		    case 0:
		        // Y Neg
		    	instance.addVertexWithUV(x + scale, y + offset, z,         1.0D, 1.0D);
		    	instance.addVertexWithUV(x + scale, y + offset, z + scale, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x,         y + offset, z + scale, 0.0D, 0.0D);
		    	instance.addVertexWithUV(x,         y + offset, z,         0.0D, 1.0D);
		    	break;
		    case 1:
		        // Y Pos
		    	instance.addVertexWithUV(x,         y + offset, z + scale, 1.0D, 1.0D);
		    	instance.addVertexWithUV(x + scale, y + offset, z + scale, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x + scale, y + offset, z,         0.0D, 0.0D);
		    	instance.addVertexWithUV(x,         y + offset, z,         0.0D, 1.0D);
		    	break;
		    case 2:
		    	// Z Neg
		    	instance.addVertexWithUV(x,         y,         z + offset, 1.0D, 1.0D);
		    	instance.addVertexWithUV(x,         y + scale, z + offset, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x + scale, y + scale, z + offset, 0.0D, 0.0D);
		    	instance.addVertexWithUV(x + scale, y,         z + offset, 0.0D, 1.0D);
		    	break;
		    case 3:
		    	// Z Pos
		    	instance.addVertexWithUV(x,         y + scale, z + offset, 1.0D, 1.0D);
		    	instance.addVertexWithUV(x,         y,         z + offset, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x + scale, y,         z + offset, 0.0D, 0.0D);
		    	instance.addVertexWithUV(x + scale, y + scale, z + offset, 0.0D, 1.0D);
		    	break;
		    case 4:
		    	// X NEG
		    	instance.addVertexWithUV(x + offset, y,         z, 1.0D, 1.0D);
		    	instance.addVertexWithUV(x + offset, y,         z + scale, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x + offset, y + scale, z + scale, 0.0D, 0.0D);
		    	instance.addVertexWithUV(x + offset, y + scale, z, 0.0D, 1.0D);
		    	break;
		    case 5:
		    	// X POS
		    	instance.addVertexWithUV(x + offset, y + scale, z,         1.0D, 1.0D);
		    	instance.addVertexWithUV(x + offset, y + scale, z + scale, 1.0D, 0.0D);
		    	instance.addVertexWithUV(x + offset, y,         z + scale, 0.0D, 0.0D);
		    	instance.addVertexWithUV(x + offset, y,         z,         0.0D, 1.0D);
		    	break;
		    default:
		}
		// @formatter:on

        instance.draw();

        glDisable(GL_BLEND);
        glPopMatrix();
    }

    private void drawPlaneYPos(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (-(y + offset));
            float f9 = f8 + ActiveRenderInfo.objectY;
            float f10 = f8 + f5 + ActiveRenderInfo.objectY;
            float f11 = f9 / f10;
            f11 += (float) (y + offset);
            glTranslatef(dX, f11, dZ);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dX, -dZ, -dY);
            glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectZ * f5 / f9, -dY);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x,         y + offset, z);
			tessellator.addVertex(x,         y + offset, z + scale);
			tessellator.addVertex(x + scale, y + offset, z + scale);
			tessellator.addVertex(x + scale, y + offset, z);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private void drawPlaneYNeg(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (y + offset);
            float f9 = f8 - ActiveRenderInfo.objectY;
            float f10 = f8 + f5 - ActiveRenderInfo.objectY;
            float f11 = f9 / f10;
            f11 += (float) (y + offset);
            glTranslatef(dX, f11, dZ);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dX, -dZ, -dY);
            glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectZ * f5 / f9, -dY);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x,         y + offset, z + scale);
			tessellator.addVertex(x,         y + offset, z       );
			tessellator.addVertex(x + scale, y + offset, z       );
			tessellator.addVertex(x + scale, y + offset, z + scale);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private void drawPlaneZPos(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (-(z + offset));
            float f9 = f8 + ActiveRenderInfo.objectZ;
            float f10 = f8 + f5 + ActiveRenderInfo.objectZ;
            float f11 = f9 / f10;
            f11 += (float) (z + offset);
            glTranslatef(dX, dY, f11);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dX, -dY, -dZ);
            glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -dZ);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x,         y + scale, z + offset);
			tessellator.addVertex(x,         y,         z + offset);
			tessellator.addVertex(x + scale, y,         z + offset);
			tessellator.addVertex(x + scale, y + scale, z + offset);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private void drawPlaneZNeg(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (z + offset);
            float f9 = f8 - ActiveRenderInfo.objectZ;
            float f10 = f8 + f5 - ActiveRenderInfo.objectZ;
            float f11 = f9 / f10;
            f11 += (float) (z + offset);
            glTranslatef(dX, dY, f11);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dX, -dY, -dZ);
            glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -dZ);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x,         y,         z + offset);
			tessellator.addVertex(x,         y + scale, z + offset);
			tessellator.addVertex(x + scale, y + scale, z + offset);
			tessellator.addVertex(x + scale, y,         z + offset);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private void drawPlaneXPos(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (-(x + offset));
            float f9 = f8 + ActiveRenderInfo.objectX;
            float f10 = f8 + f5 + ActiveRenderInfo.objectX;
            float f11 = f9 / f10;
            f11 += (float) (x + offset);
            glTranslatef(f11, dY, dZ);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dZ, -dY, -dX);
            glTranslatef(ActiveRenderInfo.objectZ * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -dX);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x + offset, y + scale, z);
			tessellator.addVertex(x + offset, y + scale, z + scale);
			tessellator.addVertex(x + offset, y,         z + scale);
			tessellator.addVertex(x + offset, y,         z);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private void drawPlaneXNeg(float dX, float dY, float dZ, double x, double y, double z, double offset, double scale) {
        for (int count = 0; count < planeCount; ++count) {
            glPushMatrix();
            float f5 = 16 - count;
            float f6 = 0.0625F;
            float f7 = 1.0F / (f5 + 1.0F);
            if (count == 0) {
                bindTexture(tunnel);
                f7 = 0.1F;
                f5 = 65.0F;
                f6 = 0.125F;
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
            if (count == 1) {
                bindTexture(particleField);
                glEnable(GL_BLEND);
                glBlendFunc(1, 1);
                f6 = 0.5F;
            }
            float f8 = (float) (x + offset);
            float f9 = f8 - ActiveRenderInfo.objectX;
            float f10 = f8 + f5 - ActiveRenderInfo.objectX;
            float f11 = f9 / f10;
            f11 += (float) (x + offset);
            glTranslatef(f11, dY, dZ);
            glTexGeni(8192, 9472, 9217);
            glTexGeni(8193, 9472, 9217);
            glTexGeni(8194, 9472, 9217);
            glTexGeni(8195, 9472, 9216);
            glTexGen(8192, 9473, calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
            glTexGen(8193, 9473, calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
            glTexGen(8194, 9473, calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            glTexGen(8195, 9474, calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
            glEnable(3168);
            glEnable(3169);
            glEnable(3170);
            glEnable(3171);
            glPopMatrix();
            glMatrixMode(5890);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.0F, fieldTranslation, 0.0F);
            glScalef(f6, f6, f6);
            glTranslatef(0.5F, 0.5F, 0.0F);
            glRotatef((count * count * 4321 + count * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
            glTranslatef(-0.5F, -0.5F, 0.0F);
            glTranslatef(-dZ, -dY, -dX);
            glTranslatef(ActiveRenderInfo.objectZ * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -dX);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            f11 = random.nextFloat() * 0.5F + 0.1F;
            float f12 = random.nextFloat() * 0.5F + 0.4F;
            float f13 = random.nextFloat() * 0.5F + 0.5F;
            if (count == 0) {
                f13 = 1.0F;
                f12 = 1.0F;
                f11 = 1.0F;
            }
            tessellator.setBrightness(fieldBrightness);
            tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
            // @formatter:off
			tessellator.addVertex(x + offset, y,         z);
			tessellator.addVertex(x + offset, y,         z + scale);
			tessellator.addVertex(x + offset, y + scale, z + scale);
			tessellator.addVertex(x + offset, y + scale, z);
			// @formatter:on
            tessellator.draw();
            glPopMatrix();
            glMatrixMode(5888);
        }
    }

    private FloatBuffer calcFloatBuffer(float f, float f1, float f2, float f3) {
        this.floatBuffer.clear();
        this.floatBuffer.put(f).put(f1).put(f2).put(f3);
        this.floatBuffer.flip();
        return this.floatBuffer;
    }
}