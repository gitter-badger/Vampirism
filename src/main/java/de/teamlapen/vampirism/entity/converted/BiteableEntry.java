package de.teamlapen.vampirism.entity.converted;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import de.teamlapen.vampirism.client.render.converted.IRenderConvertible;
import net.minecraft.entity.EntityCreature;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Created by Max on 13.08.2015.
 */
public class BiteableEntry<T extends EntityCreature> {

    public final Class<T> clazz;
    public final int max_blood;
    public final boolean convertable;
    public final static BiteableEntry<EntityCreature> defaultEntry = new BiteableEntry<EntityCreature>(EntityCreature.class, -1, false);
    private IConvertibleAnalyzer<T> analyzer;
    @SideOnly(Side.CLIENT)
    private IRenderConvertible renderer;

    public BiteableEntry(Class<T> clazz, int max_blood, boolean convertable) {
        this.clazz = clazz;
        this.max_blood = max_blood;
        this.convertable = convertable;
    }

    public void setAnalyzer(IConvertibleAnalyzer<T> analyzer) {
        this.analyzer = analyzer;
    }

    public
    @NonNull
    ConvertedExtraData<T> getData(T entity) {
        return analyzer == null ? new ConvertedExtraData<T>(0) : analyzer.analyzeCreature(entity);
    }

    public
    @NonNull
    ConvertedExtraData<T> createEmptyData() {
        return analyzer == null ? new ConvertedExtraData<T>() : analyzer.createEmptyData();
    }

    @SideOnly(Side.CLIENT)
    public void setRenderer(IRenderConvertible renderer) {
        this.renderer = renderer;
    }

    @SideOnly(Side.CLIENT)
    public IRenderConvertible getRenderer() {
        return renderer;
    }

    public String toString() {
        return String.format("Biteable Entry for %s (Blood %s, Covertable %b)", clazz, max_blood, convertable);
    }
}
