package anticope.rejects.utils.player;

import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryHelper {

    public static int getSlot() {
        return mc.player.getInventory().getSelectedSlot();
    }

    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }
        return emptySlots;
    }

    public static boolean isEmptySlot(int slotId) {
        ItemStack itemStack = mc.player.getInventory().getStack(slotId);
        return itemStack == null || itemStack.getItem() instanceof AirBlockItem;
    }

    public static boolean isOffhandEmpty() {
        ItemStack offhandIem = mc.player.getOffHandStack();
        return offhandIem == null || offhandIem.getItem() instanceof AirBlockItem;
    }


    public static int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (isEmptySlot(i)) return i;
        }
        return -1;
    }

    public static int findEmptyInventorySlot() {
        for (int i = 9; i < 36; i++) {
            if (isEmptySlot(i)) return i;
        }
        return -1;
    }


    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }

    public static String getItemName(int slot) {
        return mc.player.getInventory().getStack(slot).getName().toString();
    }


    public static Item getItemFromSlot(Integer slot) {
        if (slot == -1) return null;
        if (slot == 45) return mc.player.getOffHandStack().getItem();
        return mc.player.getInventory().getStack(slot).getItem();
    }

    public static ItemStack getOffhandItemStack() {
        return mc.player.getOffHandStack();
    }

    public static Item getOffhandItem() {
        return getOffhandItemStack().getItem();
    }

    public static Item getMainhandItem() {
        return mc.player.getMainHandStack().getItem();
    }

    public static void updateSlot(int newSlot) {
        mc.player.getInventory().setSelectedSlot(newSlot);
    }
}
