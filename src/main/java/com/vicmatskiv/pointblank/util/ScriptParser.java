package com.vicmatskiv.pointblank.util;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Logger;

public final class ScriptParser {
    public static final GroovyShell shell = new GroovyShell();
    public static final Logger LOGGER = Logger.getLogger(ScriptParser.class.getName());
    public static final HashMap<ResourceLocation, Script> SCRIPTCACHE = new HashMap<>();

    public static Script getScript(Path pathFromRun) {
        try {
            Script parse = shell.parse(FMLPaths.GAMEDIR.get().resolve(pathFromRun).toUri());
            System.out.println("Script Parsed: " + parse);
            return parse;
        } catch (IOException e) {
            LOGGER.severe("Failed to parse script: " + pathFromRun);
            throw new RuntimeException(e);
        }
    }

    public static Script getScript(Reader reader) {
        return shell.parse(reader);
    }

    public static void cacheScript(Path toScript, ResourceLocation scriptId) {
        if (SCRIPTCACHE.containsKey(scriptId)) {
            return;
        }
        try {
            Script script = shell.parse(FMLPaths.GAMEDIR.get().resolve(toScript).toUri());
            SCRIPTCACHE.put(scriptId, script);
        } catch (IOException e) {
            LOGGER.severe("Failed to parse script: " + toScript);
            throw new RuntimeException(e);
        }
    }

    public static void cacheScript(Reader scriptReader, ResourceLocation scriptId) {
        if (SCRIPTCACHE.containsKey(scriptId)) {
            return;
        }
        try {
            Script script = shell.parse(scriptReader);
            SCRIPTCACHE.put(scriptId, script);
        } catch (Exception e) {
            LOGGER.severe("Failed to parse script from zip: " + scriptId);
            throw new RuntimeException(e);
        }
    }
}
