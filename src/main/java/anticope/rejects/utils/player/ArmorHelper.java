package anticope.rejects.utils.player;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ArmorHelper {

    // todo this has some bugs..

    public static ItemStack getArmorItemStack(EquipmentSlot equipmentSlot) {
        return mc.player.getEquippedStack(equipmentSlot);
    }

    public static Item getArmorItem(EquipmentSlot equipmentSlot) {
        return getArmorItemStack(equipmentSlot).getItem();
    }

    public static boolean checkThreshold(ItemStack i, double threshold) {
        return getDamage(i) <= threshold;
    }

    public static double getDamage(ItemStack i) {
        return (((double) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage()) * 100);
    }

    public static ItemStack getArmor(int slot) {
        return switch (slot) {
            case 0 -> getFeetItemStack();
            case 1 -> getLegsItemStack();
            case 2 -> getChestItemStack();
            case 3 -> getHeadItemStack();
            default -> ItemStack.EMPTY;
        };
    }

    public static Item getHeadItem() {
        return getArmorItem(EquipmentSlot.HEAD);
    }

    public static ItemStack getHeadItemStack() {
        return getArmorItemStack(EquipmentSlot.HEAD);
    }

    public static Item getChestItem() {
        return getArmorItem(EquipmentSlot.CHEST);
    }

    public static ItemStack getChestItemStack() {
        return getArmorItemStack(EquipmentSlot.CHEST);
    }

    public static Item getLegsItem() {
        return getArmorItem(EquipmentSlot.LEGS);
    }

    public static ItemStack getLegsItemStack() {
        return getArmorItemStack(EquipmentSlot.LEGS);
    }

    public static Item getFeetItem() {
        return getArmorItem(EquipmentSlot.FEET);
    }

    public static ItemStack getFeetItemStack() {
        return getArmorItemStack(EquipmentSlot.FEET);
    }


    public static boolean hasHelmet() {
        return isHelm(getHeadItemStack());
    }

    public static boolean hasChestplate() {
        return isChest(getChestItemStack());
    }

    public static boolean hasElytra() {
        return getArmorItem(EquipmentSlot.CHEST).equals(Items.ELYTRA);
    }

    public static boolean hasLeggings() {
        return isLegs(getLegsItemStack());
    }

    public static boolean hasBoots() {
        return isBoots(getFeetItemStack());
    }


    public static boolean isHelm(ItemStack itemStack) {
        return itemStack != null && (
            itemStack.getItem() == Items.NETHERITE_HELMET ||
                itemStack.getItem() == Items.DIAMOND_HELMET ||
                itemStack.getItem() == Items.GOLDEN_HELMET ||
                itemStack.getItem() == Items.IRON_HELMET ||
                itemStack.getItem() == Items.CHAINMAIL_HELMET ||
                itemStack.getItem() == Items.LEATHER_HELMET
        );
    }

    public static boolean isChest(ItemStack itemStack) {
        return itemStack != null && (
            itemStack.getItem() == Items.NETHERITE_CHESTPLATE ||
                itemStack.getItem() == Items.DIAMOND_CHESTPLATE ||
                itemStack.getItem() == Items.GOLDEN_CHESTPLATE ||
                itemStack.getItem() == Items.IRON_CHESTPLATE ||
                itemStack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
                itemStack.getItem() == Items.LEATHER_CHESTPLATE
        );
    }

    public static boolean isLegs(ItemStack itemStack) {
        return itemStack != null && (
            itemStack.getItem() == Items.NETHERITE_LEGGINGS ||
                itemStack.getItem() == Items.DIAMOND_LEGGINGS ||
                itemStack.getItem() == Items.GOLDEN_LEGGINGS ||
                itemStack.getItem() == Items.IRON_LEGGINGS ||
                itemStack.getItem() == Items.CHAINMAIL_LEGGINGS ||
                itemStack.getItem() == Items.LEATHER_LEGGINGS
        );
    }

    public static boolean isBoots(ItemStack itemStack) {
        return itemStack != null && (
            itemStack.getItem() == Items.NETHERITE_BOOTS ||
                itemStack.getItem() == Items.DIAMOND_BOOTS ||
                itemStack.getItem() == Items.GOLDEN_BOOTS ||
                itemStack.getItem() == Items.IRON_BOOTS ||
                itemStack.getItem() == Items.CHAINMAIL_BOOTS ||
                itemStack.getItem() == Items.LEATHER_BOOTS
        );
    }
}
