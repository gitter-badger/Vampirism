package de.teamlapen.vampirism.entity.converted;

import net.minecraft.entity.EntityCreature;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Created by Max on 13.08.2015.
 */
public interface IConvertibleAnalyzer<T extends EntityCreature> {

    @NonNull
    ConvertedExtraData<T> analyzeCreature(T creature);

    @NonNull
    ConvertedExtraData<T> createEmptyData();

    Class<T> getConvertibleClass();
}
