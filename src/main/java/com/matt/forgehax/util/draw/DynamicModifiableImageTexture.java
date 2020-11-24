package com.matt.forgehax.util.draw;

import static com.matt.forgehax.Globals.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.matt.forgehax.Helper;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/*TheAlphaEpsilon - this class will change the resourcelocation if update is called,
 * Probably a better way to do this, but oh well */

public class DynamicModifiableImageTexture extends AbstractTexture {
	
	private static final ArrayList<Boolean> OPEN_NUMBERS = new ArrayList<Boolean>(Arrays.asList(new Boolean[] {true}));
	
	/**Image*/
	private BufferedImage image;
	private final int[] dynamicTextureData;
    /** width of this icon in pixels */
    private final int width;
    /** height of this icon in pixels */
    private final int height;
    /** What number this is**/
    private final int number;

    public DynamicModifiableImageTexture(BufferedImage bufferedImage) {
    	image = bufferedImage;
    	width = image.getWidth();
        height = image.getHeight();
        dynamicTextureData = new int[width * height];
        int firstOpen = OPEN_NUMBERS.indexOf(true);
        if(firstOpen > -1) {
        	number = firstOpen;
        	OPEN_NUMBERS.set(firstOpen, false);
        } else {
        	number = OPEN_NUMBERS.size();
        	OPEN_NUMBERS.add(false);
        }
        TextureUtil.allocateTexture(this.getGlTextureId(), width, height);
        bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.dynamicTextureData, 0, bufferedImage.getWidth());
        this.updateDynamicTexture();
    }

    @Override
    public void deleteGlTexture() {
        OPEN_NUMBERS.set(number, true);
        
    	super.deleteGlTexture();
    }
    
    public void loadTexture(IResourceManager resourceManager) throws IOException
    {
    }

    public void updateDynamicTexture()
    {
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), this.dynamicTextureData, 0, image.getWidth());
        TextureUtil.uploadTexture(this.getGlTextureId(), this.dynamicTextureData, this.width, this.height);
    }

    public int[] getTextureData()
    {
        return this.dynamicTextureData;
    }
    
    public ResourceLocation getResource() {
    	ResourceLocation resourcelocation = new ResourceLocation(String.format("dynamic/modifiable_image_%d", number));
        MC.getTextureManager().loadTexture(resourcelocation, this);
        return resourcelocation;
    }

}
