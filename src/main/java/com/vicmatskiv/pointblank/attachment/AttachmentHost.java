package com.vicmatskiv.pointblank.attachment;

import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public interface AttachmentHost extends ItemLike, Nameable, FeatureProvider {
   int DEFAULT_MAX_ATTACHMENT_CATEGORIES = 6;

   Collection<Attachment> getCompatibleAttachments();

   default Collection<Attachment> getDefaultAttachments() {
      return Collections.emptyList();
   }

   default Collection<AttachmentCategory> getCompatibleAttachmentCategories() {
      return this.getCompatibleAttachments().stream().map((a) -> {
         return a.getCategory();
      }).toList();
   }

   default int getMaxAttachmentCategories() {
      return 6;
   }

   default List<Component> getCompatibleAttachmentTooltipLines(ItemStack itemStack) {
      List<Component> tooltipLines = new ArrayList();
      tooltipLines.add(Component.m_237119_());
      Collection<Attachment> compatibleAttachments = this.getCompatibleAttachments();
      if (compatibleAttachments.isEmpty()) {
         tooltipLines.add(Component.m_237115_("label.pointblank.noCompatibleAttachments").m_6270_(Style.f_131099_.m_178520_(11184810).m_131155_(true).m_131162_(false)));
      } else {
         tooltipLines.add(Component.m_237115_("label.pointblank.compatibleAttachments").m_130946_(":").m_130940_(ChatFormatting.WHITE).m_130940_(ChatFormatting.ITALIC));
         Iterator var4 = compatibleAttachments.iterator();

         while(var4.hasNext()) {
            Attachment attachment = (Attachment)var4.next();
            tooltipLines.add(Component.m_237115_(attachment.m_5456_().m_5524_()).m_130940_(ChatFormatting.AQUA).m_130940_(ChatFormatting.ITALIC));
         }
      }

      return tooltipLines;
   }
}
