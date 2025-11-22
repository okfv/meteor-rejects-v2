package anticope.rejects.commands;

import anticope.rejects.arguments.EnumStringArgumentType;
import anticope.rejects.utils.GiveUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.UUID;

import static anticope.rejects.utils.accounts.GetPlayerUUID.getUUID;

public class GiveCommand extends Command {

    private final Collection<String> PRESETS = GiveUtils.PRESETS.keySet();

    public GiveCommand() {
        super("give", "Gives items in creative", "item", "kit");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // TODO : finish this
        builder.then(literal("egg").executes(ctx -> {
            ItemStack inHand = mc.player.getMainHandStack();
            ItemStack item = new ItemStack(Items.STRIDER_SPAWN_EGG);
            NbtCompound ct = new NbtCompound();

            if (inHand.getItem() instanceof BlockItem) {
                ct.putInt("Time", 1);
                ct.putString("id", "minecraft:falling_block");
                NbtCompound blockState = new NbtCompound();
                blockState.putString("Name", Registries.ITEM.getId(inHand.getItem()).toString());
                ct.put("BlockState", blockState);

            } else {
                ct.putString("id", "minecraft:item");
                NbtCompound itemTag = new NbtCompound();
                itemTag.putString("id", Registries.ITEM.getId(inHand.getItem()).toString());
                itemTag.putInt("Count", inHand.getCount());

                ct.put("Item", itemTag);
            }
            NbtCompound t = new NbtCompound();
            t.put("EntityTag", ct);

            var changes = ComponentChanges.builder()
                    .add(DataComponentTypes.CUSTOM_NAME, inHand.getName())
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(t))
                    .build();

            item.applyChanges(changes);
            GiveUtils.giveItem(item);
            return SINGLE_SUCCESS;
        }));

        //TODO: allow for custom cords to place oob, though optional args
        builder.then(literal("holo").then(argument("message", StringArgumentType.greedyString()).executes(ctx -> {
            String message = ctx.getArgument("message", String.class).replace("&", "\247");
            ItemStack stack = new ItemStack(Items.STRIDER_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            NbtList pos = new NbtList();

            pos.add(NbtDouble.of(mc.player.getX()));
            pos.add(NbtDouble.of(mc.player.getY()));
            pos.add(NbtDouble.of(mc.player.getZ()));

            tag.putString("id", "minecraft:armor_stand");
            tag.put("Pos", pos);
            tag.putBoolean("Invisible", true);
            tag.putBoolean("Invulnerable", true);
            tag.putBoolean("NoGravity", true);
            tag.putBoolean("CustomNameVisible", true);

            NbtCompound customData = new NbtCompound();
            customData.put("EntityTag", tag);

            var changes = ComponentChanges.builder()
                    .add(DataComponentTypes.CUSTOM_NAME, Text.literal(message))
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData))
                    .build();

            stack.applyChanges(changes);
            GiveUtils.giveItem(stack);
            return SINGLE_SUCCESS;
        })));

        //TODO, make invisible through potion effect
        builder.then(literal("bossbar").then(argument("message", StringArgumentType.greedyString()).executes(ctx -> {
            String message = ctx.getArgument("message", String.class).replace("&", "\247");
            ItemStack stack = new ItemStack(Items.BAT_SPAWN_EGG);
            NbtCompound tag = new NbtCompound();
            tag.putBoolean("NoAI", true);
            tag.putBoolean("Silent", true);
            tag.putBoolean("PersistenceRequired", true);
            tag.put("id", NbtString.of("minecraft:wither"));

            NbtCompound customData = new NbtCompound();
            customData.put("EntityTag", tag);

            var changes = ComponentChanges.builder()
                    .add(DataComponentTypes.CUSTOM_NAME, Text.literal(message))
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData))
                    .build();
            stack.applyChanges(changes);

            GiveUtils.giveItem(stack);
            return SINGLE_SUCCESS;
        })));

        // TODO : resolve textures, should be easy now that UUID is resolved
        builder.then(literal("head").then(argument("owner", StringArgumentType.greedyString()).executes(ctx -> {
            String playerName = ctx.getArgument("owner", String.class);
            ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
            UUID uuid = getUUID(playerName);
            NbtCompound customData = new NbtCompound();
            customData.put("SkullOwner", createSkullOwner(uuid, playerName, null));
            itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

            GiveUtils.giveItem(itemStack);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("preset").then(argument("name", new EnumStringArgumentType(PRESETS)).executes(context -> {
            String name = context.getArgument("name", String.class);
            GiveUtils.giveItem(GiveUtils.getPreset(name));
            return SINGLE_SUCCESS;
        })));
    }

    private static NbtCompound createSkullOwner(UUID uuid, String name, String textureValue) {
        NbtCompound skullOwner = new NbtCompound();
        if (uuid != null) skullOwner.putString("Id", uuid.toString());
        if (name != null && !name.isEmpty()) skullOwner.putString("Name", name);

        if (textureValue != null && !textureValue.isEmpty()) {
            NbtCompound properties = new NbtCompound();
            NbtList textures = new NbtList();
            NbtCompound value = new NbtCompound();
            value.putString("Value", textureValue);
            textures.add(value);
            properties.put("textures", textures);
            skullOwner.put("Properties", properties);
        }

        return skullOwner;
    }
}
