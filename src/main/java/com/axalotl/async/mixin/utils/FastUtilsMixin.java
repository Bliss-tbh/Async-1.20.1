package com.axalotl.async.mixin.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {
        Int2ObjectOpenHashMap.class,
        Long2ObjectOpenHashMap.class,
        LongLinkedOpenHashSet.class,
        ObjectOpenCustomHashSet.class,
        Long2LongOpenHashMap.class,
        Long2ObjectLinkedOpenHashMap.class,
        ReferenceOpenHashSet.class,
        Reference2ReferenceArrayMap.class,
        Object2LongOpenHashMap.class,
        Reference2ReferenceOpenHashMap.class,
        IntArrayList.class,
        Reference2IntOpenHashMap.class,
        ReferenceArrayList.class,
        Object2ReferenceOpenCustomHashMap.class,
        Reference2ByteOpenHashMap.class,
        Reference2LongOpenHashMap.class
},
        targets = {
                "it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$FastEntryIterator",
                "it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$MapIterator",
                "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap$MapIterator",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$ValueIterator",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeySet",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeyIterator",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapEntrySet",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$EntryIterator",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator",
                "it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator"
        }, priority = 50000)
public abstract class FastUtilsMixin {
}

