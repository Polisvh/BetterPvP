package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.InventoryAccess;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.inventory.CartographyInventory;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.map.MapIcon;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.map.MapPatch;
import me.mykindos.betterpvp.core.inventory.item.ItemWrapper;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.util.MathUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A {@link AbstractSplitWindow} that uses a {@link CartographyInventory} as the upper inventory.
 * <p>
 * Use the builder obtained by {@link CartographyWindow#single()}, to get an instance of this class.
 */
final class CartographySingleWindowImpl extends AbstractSingleWindow implements CartographyWindow {
    
    private final CartographyInventory cartographyInventory;
    private int mapId;
    
    public CartographySingleWindowImpl(
        @NotNull Player player,
        @Nullable ComponentWrapper title,
        @NotNull AbstractGui gui,
        boolean closeable
    ) {
        super(player, title, createWrappingGui(gui), null, closeable);
        if (gui.getWidth() != 2 || gui.getHeight() != 1) throw new IllegalArgumentException("Gui has to be 2x1");
        
        cartographyInventory = InventoryAccess.createCartographyInventory(player, title != null ? title.localized(player) : null);
        inventory = cartographyInventory.getBukkitInventory();
        
        resetMap();
    }
    
    private static AbstractGui createWrappingGui(Gui upperGui) {
        if (upperGui.getWidth() != 2 || upperGui.getHeight() != 1)
            throw new IllegalArgumentException("Gui has to be 2x1");
        
        Gui wrapperGui = Gui.empty(3, 1);
        wrapperGui.fillRectangle(1, 0, upperGui, true);
        return (AbstractGui) wrapperGui;
    }
    
    @Override
    public void updateMap(@Nullable MapPatch patch, @Nullable List<MapIcon> icons) {
        InventoryAccess.getPlayerUtils().sendMapUpdate(getViewer(), mapId, (byte) 0, false, patch, icons);
    }
    
    @SuppressWarnings({"deprecation", "DuplicatedCode"})
    @Override
    public void resetMap() {
        mapId = -MathUtils.RANDOM.nextInt(Integer.MAX_VALUE);
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) map.getItemMeta();
        mapMeta.setMapId(mapId);
        map.setItemMeta(mapMeta);
        getGui().setItem(0, new SimpleItem(new ItemWrapper(map)));
    }
    
    @Override
    protected void openInventory(@NotNull Player viewer) {
        cartographyInventory.open();
    }
    
    public static final class BuilderImpl
        extends AbstractBuilder<CartographyWindow, CartographyWindow.Builder.Single>
        implements CartographyWindow.Builder.Single
    {
        
        @Override
        public @NotNull CartographyWindow build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (guiSupplier == null)
                throw new IllegalStateException("Gui is not defined.");
            
            var window = new CartographySingleWindowImpl(
                viewer,
                title,
                (AbstractGui) guiSupplier.get(),
                closeable
            );
            
            applyModifiers(window);
            
            return window;
        }
        
    }
    
}
