package xyz.synse.engine.blocks;

import xyz.synse.engine.blocks.defaults.Bedrock;
import xyz.synse.engine.blocks.defaults.GrassBlock;
import xyz.synse.engine.blocks.defaults.StoneBlock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class Blocks {
    public static final Block GRASS;
    public static final Block STONE;
    public static final Block BEDROCK;
    public static HashMap<String, Class<? extends Block>> BLOCKS = new HashMap<>();

    public static Block register(Block block) {
        BLOCKS.put(block.getName(), block.getClass());

        try{
            createBlock(block.getName());
            return block;
        }catch (Exception ex){
            BLOCKS.remove(block.getName());
            throw new RuntimeException("The block doesn't have an empty constructor");
        }
    }

    public static Block createBlock(String name) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends Block> clazz = BLOCKS.get(name);

        Constructor<? extends Block> constructor = clazz.getDeclaredConstructor();

        if(!constructor.isAccessible()){
            constructor.setAccessible(true);
        }

        return constructor.newInstance();
    }

    static {
        GRASS = register(new GrassBlock());
        STONE = register(new StoneBlock());
        BEDROCK = register(new Bedrock());
    }
}
