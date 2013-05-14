/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import droidsafe.annotations.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Build;

/**
 * A ContextWrapper that allows you to modify the theme from what is in the 
 * wrapped context. 
 */
public class ContextThemeWrapper extends ContextWrapper {
   
    @DSModeled
    public ContextThemeWrapper() {
        super(null);
    }
    
    public ContextThemeWrapper(Context base, int themeres) {
        super(base);
    }
    
    @Override public void setTheme(int resid) {
    
    }
    
    protected void onApplyThemeResource(android.content.res.Resources.Theme theme,
    		int resid, boolean b) {
    	
    }
    
    /** @hide */
    @Override
    public int getThemeResId() {
        return -1;
    }

    @Override public Resources.Theme getTheme() {
   
        return null;
    }
}

