package me.mykindos.betterpvp.core.inventory.window;

import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.InventoryAccess;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.util.Pair;
import me.mykindos.betterpvp.core.inventory.util.SlotUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A {@link Window} that uses both top and player {@link Inventory}.
 * <p>
 * Only in very rare circumstances should this class be used directly.
 * Instead, use {@link Window#split()} or {@link Window#merged()} to create such a {@link Window}.
 */
public abstract class AbstractDoubleWindow extends AbstractWindow {
    
    private final Inventory playerInventory;
    private final ItemStack[] playerItems = new ItemStack[36];
    
    /**
     * The upper inventory of the window.
     */
    @Getter
    protected Inventory upperInventory;
    
    /**
     * Creates a new {@link AbstractDoubleWindow}.
     *
     * @param player         The player that views the window.
     * @param title          The title of the window.
     * @param size           The size of the window.
     * @param upperInventory The upper inventory of the window.
     * @param closeable      Whether the window is closeable.
     */
    protected AbstractDoubleWindow(Player player, ComponentWrapper title, int size, Inventory upperInventory, boolean closeable) {
        super(player, title, size, closeable);
        this.upperInventory = upperInventory;
        this.playerInventory = player.getInventory();
    }
    
    @Override
    protected void initItems() {
        // init upper inventory
        for (int i = 0; i < upperInventory.getSize(); i++) {
            SlotElement element = getSlotElement(i);
            redrawItem(i, element, true);
        }
        
        // store and clear player inventory
        Inventory inventory = getViewer().getInventory();
        for (int i = 0; i < 36; i++) {
            playerItems[i] = inventory.getItem(i);
            inventory.setItem(i, null);
        }
        
        // init player inventory
        for (int i = upperInventory.getSize(); i < upperInventory.getSize() + 36; i++) {
            SlotElement element = getSlotElement(i);
            redrawItem(i, element, true);
        }
    }
    
    @Override
    public @Nullable ItemStack @Nullable [] getPlayerItems() {
        if (isOpen())
            return playerItems;
        return null;
    }
    
    private void restorePlayerInventory() {
        Inventory inventory = getViewer().getInventory();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, playerItems[i]);
        }
    }
    
    @Override
    protected void setInvItem(int slot, ItemStack itemStack) {
        if (slot >= upperInventory.getSize()) {
            if (isOpen()) {
                int invSlot = SlotUtils.translateGuiToPlayerInv(slot - upperInventory.getSize());
                setPlayerInvItem(invSlot, itemStack);
            }
        } else setUpperInvItem(slot, itemStack);
    }
    
    /**
     * Places an {@link ItemStack} into the upper {@link Inventory}.
     *
     * @param slot      The slot in the upper {@link Inventory}.
     * @param itemStack The {@link ItemStack} to place.
     */
    protected void setUpperInvItem(int slot, ItemStack itemStack) {
        upperInventory.setItem(slot, itemStack);
    }
    
    /**
     * Places an {@link ItemStack} into the player {@link Inventory}.
     *
     * @param slot      The slot in the player {@link Inventory}.
     * @param itemStack The {@link ItemStack} to place.
     */
    protected void setPlayerInvItem(int slot, ItemStack itemStack) {
        playerInventory.setItem(slot, itemStack);
    }
    
    @Override
    public void handleViewerDeath(PlayerDeathEvent event) {
        if (isOpen()) {
            List<ItemStack> drops = event.getDrops();
            if (!event.getKeepInventory()) {
                drops.clear();
                Arrays.stream(playerItems)
                    .filter(Objects::nonNull)
                    .forEach(drops::add);
            }
        }
    }
    
    @Override
    protected void handleOpened() {
        // Prevent players from receiving advancements from UI items
        InventoryAccess.getPlayerUtils().stopAdvancementListening(getViewer());
    }
    
    @Override
    protected void handleClosed() {
        restorePlayerInventory();
        
        // Start the advancement listeners again
        InventoryAccess.getPlayerUtils().startAdvancementListening(getViewer());
    }
    
    @Override
    public void handleClick(InventoryClickEvent event) {
        Pair<AbstractGui, Integer> clicked = getWhereClicked(event);
        clicked.getFirst().handleClick(clicked.getSecond(), (Player) event.getWhoClicked(), event.getClick(), event);
    }
    
    @Override
    public void handleItemShift(InventoryClickEvent event) {
        // empty, should not be called by the WindowManager
    }
    
    @Override
    public Inventory[] getInventories() {
        return isOpen() ? new Inventory[] {upperInventory, playerInventory} : new Inventory[] {upperInventory};
    }
    
    /**
     * Gets the {@link AbstractGui} and the slot where the player clicked,
     * based on the given {@link InventoryClickEvent}.
     *
     * @param event The {@link InventoryClickEvent} that was triggered.
     * @return The {@link AbstractGui} and the slot where the player clicked.
     */
    protected abstract Pair<AbstractGui, Integer> getWhereClicked(InventoryClickEvent event);
    
}
