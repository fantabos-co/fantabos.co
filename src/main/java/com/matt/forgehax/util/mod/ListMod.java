package com.matt.forgehax.util.mod;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper;

import net.minecraft.util.text.TextFormatting;

import java.util.Comparator;

public abstract class ListMod extends HudMod {

  protected final Setting<ListSorter> sortMode;

  protected enum ListSorter {
    ALPHABETICAL((o1, o2) -> 0), // mod list is already sorted alphabetically
    LENGTH(Comparator.<String>comparingInt(SurfaceHelper::getTextWidth).reversed()),
    REVLENGTH(Comparator.<String>comparingInt(SurfaceHelper::getTextWidth));

    private final Comparator<String> comparator;

    public Comparator<String> getComparator() {
      return this.comparator;
    }

    ListSorter(Comparator<String> comparatorIn) {
      this.comparator = comparatorIn;
    }
  }

  public ListMod(Category category, String modName, boolean defaultEnabled, String description) {
    super(category, modName, defaultEnabled, description);

    this.sortMode =
        getCommandStub()
            .builders()
            .<ListSorter>newSettingEnumBuilder()
            .name("sorting")
            .description("Sorting mode")
            .defaultTo(ListSorter.LENGTH)
            .build();
  }

  public String appendArrow(final String text) {
    return AlignHelper.getFlowDirX2(alignment.get()) == 1 ? TextFormatting.GRAY + "> " + TextFormatting.WHITE + text : text + TextFormatting.GRAY + " <";
  }
}
