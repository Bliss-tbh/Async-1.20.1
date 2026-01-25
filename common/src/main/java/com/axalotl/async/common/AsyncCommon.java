package com.axalotl.async.common;

import com.axalotl.async.common.platform.PlatformEvents;
import com.axalotl.async.common.platform.PlatformUtils;

public abstract class AsyncCommon {
    public static final String MODID = "async";
    public static boolean LITHIUM = PlatformEvents.getInstance().isModLoaded("lithium");

    public final void initialize() {
        PlatformUtils.initialize();
    }
}
