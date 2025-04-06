package com.vicmatskiv.pointblank.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.InvalidExtensionComponentException;
import com.vicmatskiv.pointblank.InvalidExtensionException;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimationBuilder;
import com.vicmatskiv.pointblank.entity.EntityBuilder;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ItemBuilder;
import cpw.mods.jarhandling.SecureJar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ExtensionRegistry {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private Pack extensionsPack;
   private List<Extension> extensions;
   private RepositorySource repositorySource = new RepositorySource() {
      public void m_7686_(Consumer<Pack> consumer) {
         if (ExtensionRegistry.this.extensionsPack != null) {
            consumer.accept(ExtensionRegistry.this.extensionsPack);
         }

      }
   };

   public List<Extension> getExtensions() {
      return Collections.unmodifiableList(this.extensions);
   }

   public RepositorySource getRepositorySource() {
      return this.repositorySource;
   }

   public void discoverExtensions(PackType packType) {
      Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("pointblank");
      this.extensions = scanExtensions(resourcePacksPath);
      List<PathPackResources> extensionPacks = new ArrayList();
      Iterator var4 = this.extensions.iterator();

      while(var4.hasNext()) {
         final Extension extension = (Extension)var4.next();
         LOGGER.error("Adding extension: " + extension);
         extensionPacks.add(new PathPackResources(extension.name, false, extension.path) {
            private final SecureJar secureJar;

            {
               this.secureJar = SecureJar.from(new Path[]{extension.path});
            }

            protected Path resolve(@NotNull String... paths) {
               if (paths.length < 1) {
                  throw new IllegalArgumentException("Missing path");
               } else {
                  return this.secureJar.getPath(String.join("/", paths), new String[0]);
               }
            }

            public IoSupplier<InputStream> m_214146_(PackType type, ResourceLocation location) {
               return super.m_214146_(type, location);
            }

            public void m_8031_(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
               super.m_8031_(type, namespace, path, resourceOutput);
            }
         });
      }

      this.extensionsPack = Pack.m_245429_("pointblank_resources", Component.m_237113_("Pointblank Resources"), true, (id) -> {
         return new DelegatingPackResources(id, false, new PackMetadataSection(Component.m_237110_("pointblank.resources.modresources", new Object[]{extensionPacks.size()}), SharedConstants.m_183709_().m_264084_(packType)), extensionPacks) {
            public boolean isHidden() {
               return true;
            }
         };
      }, packType, Position.BOTTOM, PackSource.f_10527_);
   }

   public static void registerItemsFromExtensions(ExtensionRegistry registry, IEventBus modEventBus) {
      Iterator var2 = registry.getExtensions().iterator();

      while(var2.hasNext()) {
         Extension extension = (Extension)var2.next();
         List<Supplier<Item>> registeredExtItems = new ArrayList();
         ItemRegistry extItems = ItemRegistry.ITEMS;
         Iterator var6 = extension.getSounds().iterator();

         Supplier registeredExtItem;
         while(var6.hasNext()) {
            String soundName = (String)var6.next();
            registeredExtItem = SoundRegistry.register(soundName);
            extension.registeredExtSounds.put(soundName, registeredExtItem);
         }

         var6 = extension.getItemBuilders().iterator();

         while(var6.hasNext()) {
            ItemBuilder<?> itemBuilder = (ItemBuilder)var6.next();
            registeredExtItem = extItems.register(itemBuilder);
            registeredExtItems.add(registeredExtItem);
         }

         var6 = extension.getEffectBuilders().iterator();

         while(var6.hasNext()) {
            EffectBuilder<?, ?> effectBuilder = (EffectBuilder)var6.next();
            EffectRegistry.register(effectBuilder.getName(), () -> {
               return effectBuilder;
            });
         }

         var6 = extension.getPlayerAnimationBuilders().iterator();

         while(var6.hasNext()) {
            PlayerAnimationBuilder playerAnimationBuilder = (PlayerAnimationBuilder)var6.next();
            ThirdPersonAnimationRegistry.register(playerAnimationBuilder.getName(), playerAnimationBuilder.getReaderFactory());
         }

         Supplier<ItemStack> tabIconSupplier = () -> {
            Item item = null;
            if (extension.creativeTabIconItem != null) {
               Supplier<? extends Item> supplier = (Supplier)ItemRegistry.ITEMS.getItemsByName().get(extension.creativeTabIconItem);
               if (supplier != null) {
                  item = (Item)supplier.get();
               }
            }

            if (item == null) {
               Optional<Supplier<Item>> firstGunItem = registeredExtItems.stream().filter((ro) -> {
                  return ro.get() instanceof GunItem;
               }).findFirst();
               if (firstGunItem.isPresent()) {
                  item = (Item)((Supplier)firstGunItem.get()).get();
               } else {
                  item = (Item)GunRegistry.M4A1.get();
               }

               if (item == null) {
                  item = (Item)AmmoRegistry.AMMOCREATIVE.get();
               }
            }

            return new ItemStack(item != null ? item : Items.f_41852_);
         };
         ItemRegistry.TABS.register(extension.getName(), () -> {
            return CreativeModeTab.builder().m_257941_(Component.m_237115_("itemGroup." + extension.getName() + ".items")).m_257737_(tabIconSupplier).m_257501_((enabledFeatures, entries) -> {
               Output output = new Output() {
                  public void m_246267_(ItemStack itemStack, TabVisibility tabVisibility) {
                     entries.m_246267_(itemStack, tabVisibility);
                  }

                  public void m_246326_(ItemLike itemLike) {
                     if (itemLike != null) {
                        entries.m_246326_(itemLike);
                     }

                  }
               };
               Iterator var4 = registeredExtItems.iterator();

               while(var4.hasNext()) {
                  Supplier<Item> registeredExtensionItem = (Supplier)var4.next();
                  output.m_246326_((ItemLike)registeredExtensionItem.get());
               }

            }).m_257652_();
         });
      }

   }

   private static List<Extension> scanExtensions(Path extensionsPath) {
      ArrayList extensions = new ArrayList();

      try {
         DirectoryStream stream = Files.newDirectoryStream(extensionsPath);

         try {
            Iterator var3 = stream.iterator();

            while(var3.hasNext()) {
               Path entry = (Path)var3.next();
               Extension extension;
               if (Files.isDirectory(entry, new LinkOption[0])) {
                  try {
                     extension = Extension.fromPath(entry);
                     if (extension != null) {
                        extensions.add(extension);
                     }
                  } catch (Exception var8) {
                     throw new InvalidExtensionException(entry, var8);
                  }
               } else if (entry.toString().endsWith(".zip")) {
                  try {
                     extension = Extension.fromZipPath(entry);
                     if (extension != null) {
                        extensions.add(extension);
                     }
                  } catch (Exception var7) {
                     throw new InvalidExtensionException(entry, var7);
                  }
               }
            }
         } catch (Throwable var9) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (IOException var10) {
         var10.printStackTrace();
      }

      return extensions;
   }

   private static Set<String> getSoundEventNames(Path pathToJson) {
      HashSet soundEventNames = new HashSet();

      try {
         BufferedReader reader = Files.newBufferedReader(pathToJson);

         try {
            JsonElement element = (JsonElement)(new Gson()).fromJson(reader, JsonElement.class);
            if (element != null && element.isJsonObject()) {
               JsonObject jsonObject = element.getAsJsonObject();
               Iterator var5 = jsonObject.keySet().iterator();

               while(var5.hasNext()) {
                  String key = (String)var5.next();
                  soundEventNames.add(key);
               }
            } else {
               LOGGER.error("Content of the sounds file " + pathToJson + " is not an object");
            }
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return soundEventNames;
      } catch (IOException var9) {
         LOGGER.error("Failed to load json file {}", pathToJson, var9);
         throw new RuntimeException("Failed to load file " + pathToJson + ". Error: " + var9);
      }
   }

   private static Set<String> getSoundEventNamesFromZip(ZipFile zipFile, ZipEntry entry) {
      HashSet soundEventNames = new HashSet();

      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));

         try {
            JsonObject jsonObject = (JsonObject)(new Gson()).fromJson(reader, JsonObject.class);
            Iterator var5 = jsonObject.keySet().iterator();

            while(var5.hasNext()) {
               String key = (String)var5.next();
               soundEventNames.add(key);
            }
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
         return soundEventNames;
      } catch (IOException var9) {
         var9.printStackTrace();
         return null;
      }
   }

   static class Extension {
      private String name;
      private Path path;
      private String creativeTabIconItem;
      private List<ItemBuilder<?>> itemBuilders;
      private List<EffectBuilder<?, ?>> effectBuilders;
      private List<EntityBuilder<?, ?>> entityBuilders;
      private List<PlayerAnimationBuilder> playerAnimationBuilders;
      private Set<String> sounds;
      private Map<String, Supplier<SoundEvent>> registeredExtSounds;

      public Extension(String name, Path path, String creativeTabIconItem) {
         this.name = name;
         this.path = path;
         this.creativeTabIconItem = creativeTabIconItem;
      }

      List<ItemBuilder<?>> getItemBuilders() {
         return Collections.unmodifiableList(this.itemBuilders);
      }

      List<EffectBuilder<?, ?>> getEffectBuilders() {
         return this.effectBuilders;
      }

      List<EntityBuilder<?, ?>> getEntityBuilders() {
         return this.entityBuilders;
      }

      Set<String> getSounds() {
         return Collections.unmodifiableSet(this.sounds);
      }

      List<PlayerAnimationBuilder> getPlayerAnimationBuilders() {
         return this.playerAnimationBuilders;
      }

      String getName() {
         return this.name;
      }

      public String getCreativeTabIconItem() {
         return this.creativeTabIconItem;
      }

      static Extension fromPath(Path extPath) throws InvalidExtensionComponentException {
         Path extDescriptorPath = extPath.resolve("ext.json");
         if (Files.exists(extDescriptorPath, new LinkOption[0]) && Files.isRegularFile(extDescriptorPath, new LinkOption[0])) {
            try {
               BufferedReader reader = Files.newBufferedReader(extDescriptorPath);

               Extension var26;
               try {
                  Extension extension = (Extension)(new Gson()).fromJson(reader, Extension.class);
                  extension.path = extPath;
                  extension.itemBuilders = new ArrayList();
                  extension.effectBuilders = new ArrayList();
                  extension.entityBuilders = new ArrayList();
                  extension.registeredExtSounds = new HashMap();
                  extension.playerAnimationBuilders = new ArrayList();
                  Path namespacePath = extPath.resolve("assets").resolve("pointblank");
                  Path itemsPath = namespacePath.resolve("items");
                  Path playerAnimationPath;
                  if (Files.exists(itemsPath, new LinkOption[0]) && Files.isDirectory(itemsPath, new LinkOption[0])) {
                     DirectoryStream itemFiles = Files.newDirectoryStream(itemsPath, "*.json");

                     try {
                        Iterator var7 = itemFiles.iterator();

                        while(var7.hasNext()) {
                           playerAnimationPath = (Path)var7.next();
                           extension.itemBuilders.add(ItemBuilder.fromPath(playerAnimationPath));
                        }
                     } catch (Throwable var18) {
                        if (itemFiles != null) {
                           try {
                              itemFiles.close();
                           } catch (Throwable var13) {
                              var18.addSuppressed(var13);
                           }
                        }

                        throw var18;
                     }

                     if (itemFiles != null) {
                        itemFiles.close();
                     }
                  }

                  Path effectsPath = namespacePath.resolve("effects");
                  if (Files.exists(effectsPath, new LinkOption[0]) && Files.isDirectory(effectsPath, new LinkOption[0])) {
                     DirectoryStream effectFiles = Files.newDirectoryStream(effectsPath, "*.json");

                     try {
                        Iterator var24 = effectFiles.iterator();

                        while(var24.hasNext()) {
                           Path effectFile = (Path)var24.next();
                           extension.effectBuilders.add(EffectBuilder.fromPath(effectFile));
                        }
                     } catch (Throwable var17) {
                        if (effectFiles != null) {
                           try {
                              effectFiles.close();
                           } catch (Throwable var14) {
                              var17.addSuppressed(var14);
                           }
                        }

                        throw var17;
                     }

                     if (effectFiles != null) {
                        effectFiles.close();
                     }
                  }

                  Path soundsPath = namespacePath.resolve("sounds.json");
                  if (Files.exists(soundsPath, new LinkOption[0]) && Files.isRegularFile(soundsPath, new LinkOption[0])) {
                     extension.sounds = ExtensionRegistry.getSoundEventNames(soundsPath);
                  } else {
                     extension.sounds = Collections.emptySet();
                  }

                  playerAnimationPath = namespacePath.resolve("animations").resolve("player");
                  if (Files.exists(playerAnimationPath, new LinkOption[0]) && Files.isDirectory(playerAnimationPath, new LinkOption[0])) {
                     DirectoryStream playerAnimationFiles = Files.newDirectoryStream(playerAnimationPath, "*.animation.json");

                     try {
                        Iterator var10 = playerAnimationFiles.iterator();

                        while(var10.hasNext()) {
                           Path playerAnimationFile = (Path)var10.next();
                           extension.playerAnimationBuilders.add(PlayerAnimationBuilder.fromPath(playerAnimationFile));
                        }
                     } catch (Throwable var16) {
                        if (playerAnimationFiles != null) {
                           try {
                              playerAnimationFiles.close();
                           } catch (Throwable var15) {
                              var16.addSuppressed(var15);
                           }
                        }

                        throw var16;
                     }

                     if (playerAnimationFiles != null) {
                        playerAnimationFiles.close();
                     }
                  }

                  var26 = extension;
               } catch (Throwable var19) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var12) {
                        var19.addSuppressed(var12);
                     }
                  }

                  throw var19;
               }

               if (reader != null) {
                  reader.close();
               }

               return var26;
            } catch (IOException var20) {
               ExtensionRegistry.LOGGER.error("Failed to load extension from path {}. Error: {}", extPath, var20);
               return null;
            }
         } else {
            return null;
         }
      }

      static Extension fromZipPath(Path zipPath) throws InvalidExtensionComponentException {
         try {
            ZipFile zipFile = new ZipFile(zipPath.toFile());

            BufferedReader reader;
            label87: {
               Extension var7;
               try {
                  ZipEntry extDescriptorEntry = zipFile.getEntry("ext.json");
                  if (extDescriptorEntry == null) {
                     reader = null;
                     break label87;
                  }

                  reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(extDescriptorEntry)));

                  try {
                     Extension extension = (Extension)(new Gson()).fromJson(reader, Extension.class);
                     extension.path = zipPath;
                     extension.itemBuilders = new ArrayList();
                     extension.effectBuilders = new ArrayList();
                     extension.entityBuilders = new ArrayList();
                     extension.registeredExtSounds = new HashMap();
                     extension.playerAnimationBuilders = new ArrayList();
                     Enumeration entries = zipFile.entries();

                     while(true) {
                        ZipEntry entry;
                        if (!entries.hasMoreElements()) {
                           entry = zipFile.getEntry("assets/pointblank/sounds.json");
                           if (entry != null) {
                              extension.sounds = ExtensionRegistry.getSoundEventNamesFromZip(zipFile, entry);
                           }

                           var7 = extension;
                           break;
                        }

                        entry = (ZipEntry)entries.nextElement();
                        if (entry.getName().startsWith("assets/pointblank/items/") && entry.getName().endsWith(".json")) {
                           extension.itemBuilders.add(ItemBuilder.fromZipEntry(zipFile, entry));
                        } else if (entry.getName().startsWith("assets/pointblank/effects/") && entry.getName().endsWith(".json")) {
                           extension.effectBuilders.add(EffectBuilder.fromZipEntry(zipFile, entry));
                        } else if (entry.getName().startsWith("assets/pointblank/entities/") && entry.getName().endsWith(".json")) {
                           extension.entityBuilders.add(EntityBuilder.fromZipEntry(zipFile, entry));
                        } else if (entry.getName().startsWith("assets/pointblank/animations/player/") && entry.getName().endsWith(".animation.json")) {
                           extension.playerAnimationBuilders.add(PlayerAnimationBuilder.fromZipEntry(zipFile, entry));
                        }
                     }
                  } catch (Throwable var10) {
                     try {
                        reader.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }

                     throw var10;
                  }

                  reader.close();
               } catch (Throwable var11) {
                  try {
                     zipFile.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }

                  throw var11;
               }

               zipFile.close();
               return var7;
            }

            zipFile.close();
            return reader;
         } catch (IOException var12) {
            ExtensionRegistry.LOGGER.error("Failed to load extension from ZIP {}. Error: {}", zipPath, var12);
            return null;
         }
      }
   }
}
