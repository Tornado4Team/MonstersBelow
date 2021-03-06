package net.tornado.kaijumod.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.tornado.kaijumod.item.inventory.ImplementedInventory;
import net.tornado.kaijumod.recipe.BoxRecipe;
import net.tornado.kaijumod.screen.BoxScreenHandler;
import net.tornado.kaijumod.KaijuMod;

import java.util.Optional;
import java.util.Random;

public class BoxBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 63;

    public BoxBlockEntity(BlockPos pos, BlockState state) {
        super(KaijuMod.BOX_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int index) {
                switch (index) {
                    case 0: return BoxBlockEntity.this.progress;
                    case 1: return BoxBlockEntity.this.maxProgress;
                    default: return 0;
                }
            }
            public void set(int index, int value) {
                switch(index) {
                    case 0: BoxBlockEntity.this.progress = value; break;
                    case 1: BoxBlockEntity.this.maxProgress = value; break;
                }
            }

            public int size() {
                return 2;
            }
        };
    }



    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;

    }


    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BoxScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, this.inventory);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
        //return nbt;
    }

    public static void tick(World world, BlockPos pos, BlockState state, BoxBlockEntity entity) {
        if(hasRecipe(entity)&& world.isReceivingRedstonePower(pos)) {

            entity.progress++;
            if(entity.progress > entity.maxProgress) {
                craftItem(entity);
            }
        } else {
            entity.resetProgress();
        }
    }

    private static boolean hasRecipe(BoxBlockEntity entity) {
        World world = entity.world;
        SimpleInventory inventory = new SimpleInventory(entity.inventory.size());
        for (int i = 0; i < entity.inventory.size(); i++) {
            inventory.setStack(i, entity.getStack(i));
        }

        Optional<BoxRecipe> match = world.getRecipeManager()
                .getFirstMatch(BoxRecipe.Type.INSTANCE, inventory, world);

        return match.isPresent()
                && canInsertAmountIntoOutputSlot(inventory)
                && canInsertItemIntoOutputSlot(inventory, match.get().getOutput());
    }

    private static boolean canInsertItemIntoOutputSlot(SimpleInventory inventory, ItemStack output) {
        return inventory.getStack(2).getItem() == output.getItem() || inventory.getStack(2).isEmpty();
    }

    private static boolean canInsertAmountIntoOutputSlot(SimpleInventory inventory) {
        return inventory.getStack(2).getMaxCount() > inventory.getStack(2).getCount();
    }

    private static void craftItem(BoxBlockEntity entity) {
        World world = entity.world;
        SimpleInventory inventory = new SimpleInventory(entity.inventory.size());
        for (int i = 0; i < entity.inventory.size(); i++) {
            inventory.setStack(i, entity.getStack(i));
        }

        Optional<BoxRecipe> match = world.getRecipeManager()
                .getFirstMatch(BoxRecipe.Type.INSTANCE, inventory, world);

        if(match.isPresent()) {
            entity.removeStack(0,1);
           // entity.getStack(2).damage(1, new Random(), null);
            entity.getStack(2).decrement(1);


            entity.setStack(2, new ItemStack(match.get().getOutput().getItem(),
                    entity.getStack(2).getCount() + 1));

            entity.resetProgress();
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private static boolean hasNotReachedStackLimit(BoxBlockEntity entity) {
        return entity.getStack(2).getCount() < entity.getStack(2).getMaxCount();
    }

}