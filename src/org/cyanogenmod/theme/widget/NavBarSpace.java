/*
 * Copyright (C) 2016 Cyanogen, Inc.
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.cyanogenmod.theme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import org.cyanogenmod.theme.util.Utils;

/**
 * A simple view used to pad layouts so that content floats above the
 * navigation bar.  This is best used with transparent or translucent
 * navigation bars where the content can go behind them.
 */
public class NavBarSpace extends View {

    public NavBarSpace(Context context) {
        this(context, null);
    }

    public NavBarSpace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavBarSpace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!Utils.hasNavigationBar(getContext())) {
            this.setVisibility(View.GONE);
        } else {
            this.setVisibility(View.VISIBLE);
        }
    }
}
