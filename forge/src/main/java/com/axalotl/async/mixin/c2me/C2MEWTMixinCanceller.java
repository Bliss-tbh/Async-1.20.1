package com.axalotl.async.mixin.c2me;

import com.axalotl.async.Async;
import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class C2MEWTMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (Async.c2me) {
            return mixinClassName.equals("com.ishland.c2me.opts.chunk_access.mixin.async_chunk_request.MixinServerChunkManager");
        } else return false;
    }
}

