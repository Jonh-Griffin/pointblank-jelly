package com.vicmatskiv.pointblank.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraftforge.fml.loading.FMLPaths;

public class ConfigManager {
   private final Map<String, ConfigOptionBuilder<?, ?>> optionBuilders;
   private final FutureOptionResolver futureOptionResolver;

   private ConfigManager(Map<String, ConfigOptionBuilder<?, ?>> optionBuilders, FutureOptionResolver futureOptionResolver) {
      this.optionBuilders = optionBuilders;
      this.futureOptionResolver = futureOptionResolver;
      this.handleConfigFile();
   }

   private void handleConfigFile() {
      Path gameDir = FMLPaths.GAMEDIR.get();
      Path configDirectory = gameDir.resolve("config");
      Path configPath = configDirectory.resolve("pointblank-items.toml");
      Map<String, ConfigOption<?>> configFileOptions = Files.exists(configPath, new LinkOption[0]) ? parseConfig(configPath.toString(), this.optionBuilders) : new LinkedHashMap();
      Map<String, ConfigOption<?>> merged = this.merge((Map)configFileOptions);
      if (!merged.equals(configFileOptions)) {
         try {
            Files.createDirectories(configDirectory);
         } catch (IOException var7) {
            throw new RuntimeException(var7);
         }

         writeConfig(configPath, merged);
      }

      this.futureOptionResolver.options.clear();
      this.futureOptionResolver.options.putAll(merged);
   }

   private Map<String, ConfigOption<?>> merge(Map<String, ConfigOption<?>> configFileOptions) {
      List<ConfigOption<?>> declaredOptions = new ArrayList();
      Iterator var3 = this.optionBuilders.values().iterator();

      while(var3.hasNext()) {
         ConfigOptionBuilder<?, ?> optionBuilder = (ConfigOptionBuilder)var3.next();
         ConfigOption<?> refOption = optionBuilder.build();
         declaredOptions.add(refOption);
      }

      List<ConfigOption<?>> merged = ConfigUtil.mergeCollections(declaredOptions, configFileOptions.values(), ConfigOption::getName, Comparator.comparing(ConfigOption::getIndex), (refOptionx, fileOption) -> {
         if (refOptionx == null) {
            return null;
         } else {
            return fileOption == null ? refOptionx : refOptionx.copy(fileOption, fileOption.get());
         }
      });
      return (Map)merged.stream().collect(Collectors.toMap(ConfigOption::getName, (b) -> {
         return b;
      }, (existing, replacement) -> {
         return replacement;
      }, LinkedHashMap::new));
   }

   private static void writeConfig(Path path, Map<String, ConfigOption<?>> options) {
      Map<String, Map<String, ConfigOption<?>>> tables = new LinkedHashMap();

      Entry entry;
      String table;
      String key;
      for(Iterator var3 = options.entrySet().iterator(); var3.hasNext(); ((Map)tables.computeIfAbsent(table, (k) -> {
         return new LinkedHashMap();
      })).put(key, (ConfigOption)entry.getValue())) {
         entry = (Entry)var3.next();
         String fullPath = (String)entry.getKey();
         int lastDotIndex = fullPath.lastIndexOf(46);
         if (lastDotIndex == -1) {
            table = "";
            key = fullPath;
         } else {
            table = fullPath.substring(0, lastDotIndex);
            key = fullPath.substring(lastDotIndex + 1);
         }
      }

      try {
         BufferedWriter br = new BufferedWriter(new FileWriter(path.toString()));

         try {
            Iterator var17 = tables.entrySet().iterator();

            while(var17.hasNext()) {
               Entry<String, Map<String, ConfigOption<?>>> tableEntry = (Entry)var17.next();
               String table = (String)tableEntry.getKey();
               Map<String, ConfigOption<?>> tableOptions = (Map)tableEntry.getValue();
               if (!table.isEmpty()) {
                  br.write("[" + table + "]");
                  br.write(System.lineSeparator());
               }

               Iterator var21 = tableOptions.entrySet().iterator();

               while(var21.hasNext()) {
                  Entry<String, ConfigOption<?>> optionEntry = (Entry)var21.next();
                  ConfigOption<?> option = (ConfigOption)optionEntry.getValue();
                  Iterator var11 = option.getSerialized().iterator();

                  while(var11.hasNext()) {
                     String s = (String)var11.next();
                     br.write(s);
                     br.write(System.lineSeparator());
                  }
               }

               br.write(System.lineSeparator());
            }
         } catch (Throwable var14) {
            try {
               br.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }

            throw var14;
         }

         br.close();
      } catch (IOException var15) {
         throw new RuntimeException(var15);
      }
   }

   private static Map<String, ConfigOption<?>> parseConfig(String filePath, Map<String, ConfigOptionBuilder<?, ?>> optionBuilders) {
      Map<String, ConfigOption<?>> options = new LinkedHashMap();
      List<String> precedingLines = new ArrayList();
      String currentPath = "";

      try {
         BufferedReader br = new BufferedReader(new FileReader(filePath));

         try {
            int optionIndex = 0;

            label53:
            while(true) {
               while(true) {
                  String line;
                  if ((line = br.readLine()) == null) {
                     break label53;
                  }

                  line = line.trim();
                  if (line.startsWith("#")) {
                     precedingLines.add(line.substring(1).trim());
                  } else if (line.startsWith("[") && line.endsWith("]")) {
                     currentPath = line.substring(1, line.length() - 1).trim();
                  } else if (line.contains("=")) {
                     String[] parts = line.split("=", 2);
                     String key = parts[0].trim();
                     String value = parts[1].trim().replaceAll("^\"|\"$", "");
                     String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                     ConfigOptionBuilder<?, ?> optionBuilder = (ConfigOptionBuilder)optionBuilders.get(fullPath);
                     if (optionBuilder != null) {
                        ConfigOption<?> option = optionBuilder.build(value, precedingLines, optionIndex);
                        options.put(fullPath, option);
                     } else {
                        options.put(fullPath, new UnknownOption(fullPath, optionIndex, value, precedingLines));
                     }

                     precedingLines.clear();
                     ++optionIndex;
                  }
               }
            }
         } catch (Throwable var15) {
            try {
               br.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         br.close();
      } catch (IOException var16) {
         var16.printStackTrace();
      }

      return options;
   }

   private static class FutureOptionResolver {
      private final Map<String, ConfigOption<?>> options = new LinkedHashMap();

      public Object getValue(String name) {
         ConfigOption<?> option = (ConfigOption)this.options.get(name);
         return option != null ? option.get() : null;
      }
   }

   public static class Builder {
      private int optionCounter;
      private final List<ConfigOptionBuilder<?, ?>> optionBuilders = new ArrayList();
      private final FutureOptionResolver futureOptionResolver = new FutureOptionResolver();

      public <N extends Number, B extends ConfigOptionBuilder<N, B>> ConfigOptionBuilder<N, B> createNumberOption(Class<N> cls, Function<String, N> converter) {
         int optionIndex = this.optionCounter++;
         Function<String, Number> futureOptionResolver = (name) -> {
            return (Number)this.futureOptionResolver.getValue(name);
         };
         ConfigOptionBuilder<N, B> optionBuilder = NumberOption.builder(cls, converter, futureOptionResolver, optionIndex);
         this.optionBuilders.add(optionBuilder);
         return optionBuilder;
      }

      public <B extends ConfigOptionBuilder<Integer, B>> ConfigOptionBuilder<Integer, B> createIntOption() {
         return this.createNumberOption(Integer.class, Integer::parseInt);
      }

      public <B extends ConfigOptionBuilder<Double, B>> ConfigOptionBuilder<Double, B> createDoubleOption() {
         return this.createNumberOption(Double.class, Double::parseDouble);
      }

      public <B extends ConfigOptionBuilder<Boolean, B>> ConfigOptionBuilder<Boolean, B> createBooleanOption() {
         int optionIndex = this.optionCounter++;
         Function<String, Boolean> futureOptionResolver = (name) -> {
            return (Boolean)this.futureOptionResolver.getValue(name);
         };
         ConfigOptionBuilder<Boolean, B> optionBuilder = BooleanOption.builder(futureOptionResolver, optionIndex);
         this.optionBuilders.add(optionBuilder);
         return optionBuilder;
      }

      public <T extends Enum<T>, B extends ConfigOptionBuilder<T, B>> ConfigOptionBuilder<T, B> createEnumOption(Class<T> cls) {
         int optionIndex = this.optionCounter++;
         Function<String, T> futureOptionResolver = (name) -> {
            return (Enum)cls.cast(this.futureOptionResolver.getValue(name));
         };
         ConfigOptionBuilder<T, B> optionBuilder = EnumOption.builder(cls, futureOptionResolver, optionIndex);
         this.optionBuilders.add(optionBuilder);
         return optionBuilder;
      }

      public ConfigManager build() {
         return new ConfigManager((Map)this.optionBuilders.stream().collect(Collectors.toMap(ConfigOptionBuilder::getName, (b) -> {
            return b;
         }, (existing, replacement) -> {
            return replacement;
         }, LinkedHashMap::new)), this.futureOptionResolver);
      }
   }
}
