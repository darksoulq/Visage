package com.github.darksoulq.visage.block;

import com.github.darksoulq.abyssallib.common.serialization.Codecs;
import com.github.darksoulq.abyssallib.world.block.property.Property;

import java.util.Arrays;
import java.util.EnumSet;

public class AttachmentProperty extends Property<Attachment> {

    public static final EnumSet<Attachment> ALL = EnumSet.allOf(Attachment.class);

    private final EnumSet<Attachment> allowedAttachments;

    private AttachmentProperty(Attachment initialValue, EnumSet<Attachment> allowedAttachments) {
        super(Codecs.STRING.xmap(s -> Attachment.valueOf(s.toUpperCase()), Attachment::name), initialValue);
        this.allowedAttachments = allowedAttachments;
    }

    public static AttachmentProperty all(Attachment initialValue) {
        return new AttachmentProperty(initialValue, ALL.clone());
    }

    public static AttachmentProperty of(Attachment initialValue, Attachment... attachments) {
        return new AttachmentProperty(initialValue, EnumSet.copyOf(Arrays.asList(attachments)));
    }

    public EnumSet<Attachment> getAllowedAttachments() {
        return allowedAttachments;
    }

    @Override
    public void set(Attachment value) {
        if (!allowedAttachments.contains(value)) {
            throw new IllegalArgumentException("Attachment " + value + " is not allowed for this property.");
        }
        super.set(value);
    }
}