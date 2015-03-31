/*
 * Copyright (C) 2015 Cyanogen, Inc.
 */
package com.cyngn.theme.perapptheming;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ThemeChangeRequest;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ThemesContract.ThemesColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import android.widget.Toast;
import com.cyngn.theme.chooser.R;
import com.cyngn.theme.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PerAppThemingWindow extends Service implements OnTouchListener,
        ThemeManager.ThemeChangeListener {
    // Animation frame rate per second
    private static final int ANIMATION_FRAME_RATE = 60;

    private static final int EXIT_DELETE_MODE_ANIMATION_DURATION = 50;

    private static final int MOVE_TO_DELETE_BOX_ANIMATION_DURATION = 150;

    private static final int ANIMATION_DURATION = 300;

    private static final int LIST_ON_LEFT_SIDE = 0;
    private static final int LIST_ON_RIGHT_SIDE = 1;

    // Don't want these colors to be themable and possibly alter the effect we are after, so
    // they are defined here rather than in colors.xml
    private static final int SCRIM_COLOR_TRANSPARENT = 0x00000000;
    private static final int SCRIM_COLOR_OPAQUE = 0xaa000000;

    // Amount to wait after a theme change occurred before fading the scrim away
    // This value was obtained empirically by performing theme changes and adjusting this delay
    private static final int THEME_CHANGE_DELAY = 1500;

    private static final float PRESSED_FAB_SCALE = 0.95f;

    private static final float DELETE_BOX_ANIMATION_SCALE = 0.3f;

    private static final int MAX_DEPRECIATION = 5;

    // Margin around the phone
    private static int MARGIN_VERTICAL;
    // Margin around the phone
    private static int MARGIN_HORIZONTAL;
    private static int CLOSE_ANIMATION_DISTANCE;
    private static int DRAG_DELTA;
    private static int STARTING_POINT_Y;
    private static int DELETE_BOX_WIDTH;
    private static int DELETE_BOX_HEIGHT;
    private static int FLOATING_WINDOW_ICON_SIZE;

    // View variables
    private BroadcastReceiver mBroadcastReceiver;
    private WindowManager mWindowManager;
    private LinearLayout mDraggableIcon;
    private View mDraggableIconImage;
    private WindowManager.LayoutParams mParams;
    private PerAppThemeListLayout mThemeListLayout;
    private WindowManager.LayoutParams mListLayoutParams;
    private ListView mThemeList;
    private ThemesAdapter mAdapter;
    private FrameLayout.LayoutParams mListParams;
    private LinearLayout mDeleteView;
    private View mDeleteBoxView;
    private View mThemeApplyingView;
    private boolean mDeleteBoxVisible = false;
    private boolean mIsDestroyed = false;
    private boolean mIsBeingDestroyed = false;
    private int mCurrentPosX = -1;

    // Animation variables
    private List<Float> mDeltaXArray;
    private List<Float> mDeltaYArray;
    private AnimationTask mAnimationTask;

    // Close logic
    private int mCurrentX;
    private int mCurrentY;
    private boolean mIsInDeleteMode = false;
    private boolean mIsAnimationLocked = false;

    // Drag variables
    float mPrevDragX;
    float mPrevDragY;
    float mOrigX;
    float mOrigY;
    boolean mDragged;

    private ThemeConfig mThemeConfig;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Load margins, distances, etc.
        final Resources res = getResources();
        MARGIN_VERTICAL =
                res.getDimensionPixelSize(R.dimen.floating_window_margin_vertical);
        MARGIN_HORIZONTAL =
                res.getDimensionPixelSize(R.dimen.floating_window_margin_horizontal);
        CLOSE_ANIMATION_DISTANCE =
                res.getDimensionPixelSize(R.dimen.floating_window_close_animation_distance);
        DRAG_DELTA = res.getDimensionPixelSize(R.dimen.floating_window_drag_delta);
        STARTING_POINT_Y = res.getDimensionPixelSize(R.dimen.floating_window_starting_point_y);

        DELETE_BOX_WIDTH = (int) getResources().getDimension(
                R.dimen.floating_window_delete_box_width);
        DELETE_BOX_HEIGHT = (int) getResources().getDimension(
                R.dimen.floating_window_delete_box_height);
        FLOATING_WINDOW_ICON_SIZE = (int) getResources().getDimension(
                R.dimen.floating_window_icon);

        mDraggableIcon = new LinearLayout(this);
        mDraggableIcon.setOnTouchListener(this);
        View.inflate(getContext(), R.layout.per_app_fab_floating_window_icon, mDraggableIcon);
        mDraggableIconImage = mDraggableIcon.findViewById(R.id.box);
        mParams = addView(mDraggableIcon, 0, 0);
        updateIconPosition(MARGIN_HORIZONTAL, STARTING_POINT_Y);

        mThemeListLayout = (PerAppThemeListLayout) View.inflate(getContext(),
                R.layout.per_app_theme_list, null);
        mThemeListLayout.setPerAppThemingWindow(this);
        mThemeList = (ListView) mThemeListLayout.findViewById(R.id.theme_list);
        mListParams = (FrameLayout.LayoutParams) mThemeList.getLayoutParams();
        mThemeApplyingView = mThemeListLayout.findViewById(R.id.applying_theme_text);

        final Configuration config = getResources().getConfiguration();
        mThemeConfig = config != null ? config.themeConfig : null;
        loadThemes();
        getContentResolver().registerContentObserver(ThemesColumns.CONTENT_URI, true,
                mThemesObserver);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPrevDragX = mOrigX = event.getRawX();
                mPrevDragY = mOrigY = event.getRawY();

                mDragged = false;

                mDeltaXArray = new LinkedList<Float>();
                mDeltaYArray = new LinkedList<Float>();

                mCurrentX = mParams.x;
                mCurrentY = mParams.y;

                mDraggableIconImage.setScaleX(PRESSED_FAB_SCALE);
                mDraggableIconImage.setScaleY(PRESSED_FAB_SCALE);

                // Cancel any currently running animations
                if (mAnimationTask != null) {
                    mAnimationTask.cancel();
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsAnimationLocked = false;
                if (mAnimationTask != null) {
                    mAnimationTask.cancel();
                }

                if (!mDragged) {
                    // clicked so show theme list
                    final int mid = getScreenWidth() / 2;
                    int side = LIST_ON_LEFT_SIDE;
                    if (mCurrentPosX > mid) side = LIST_ON_RIGHT_SIDE;
                    if (!mThemeListLayout.isAttachedToWindow()) showThemeList(side);
                } else {
                    // Animate the icon
                    mAnimationTask = new AnimationTask();
                    mAnimationTask.run();
                }

                if (mIsInDeleteMode) {
                    close(true);
                } else {
                    hideDeleteBox();
                    mDraggableIconImage.setScaleX(1f);
                    mDraggableIconImage.setScaleY(1f);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentX = (int) (event.getRawX() - mDraggableIcon.getWidth() / 2);
                mCurrentY = (int) (event.getRawY() - mDraggableIcon.getHeight());
                if (isDeleteMode(mCurrentX, mCurrentY)) {
                    if (!mIsInDeleteMode) {
                        animateToDeleteBoxCenter(null);
                    }
                } else if (isDeleteMode() && !mIsAnimationLocked) {
                    mIsInDeleteMode = false;
                    if (mAnimationTask != null) {
                        mAnimationTask.cancel();
                    }

                    mAnimationTask = new AnimationTask(mCurrentX, mCurrentY);
                    mAnimationTask.setDuration(EXIT_DELETE_MODE_ANIMATION_DURATION);
                    mAnimationTask.setInterpolator(new LinearInterpolator());
                    mAnimationTask.setAnimationFinishedListener(new OnAnimationFinishedListener() {
                        @Override
                        public void onAnimationFinished() {
                            mIsAnimationLocked = false;
                        }
                    });

                    mAnimationTask.run();
                    mIsAnimationLocked = true;
                    mDeleteBoxView.setBackgroundResource(R.drawable
                            .btn_quicktheme_remove_normal);
                } else {
                    if (mIsInDeleteMode) {
                        mDeleteBoxView.setBackgroundResource(R.drawable
                                .btn_quicktheme_remove_normal);
                        mIsInDeleteMode = false;
                    } if(!mIsAnimationLocked && mDragged) {
                        if (mAnimationTask != null) {
                            mAnimationTask.cancel();
                        }

                        updateIconPosition(mCurrentX, mCurrentY);
                    }
                }

                float deltaX = event.getRawX() - mPrevDragX;
                float deltaY = event.getRawY() - mPrevDragY;

                mDeltaXArray.add(deltaX);
                mDeltaYArray.add(deltaY);

                mPrevDragX = event.getRawX();
                mPrevDragY = event.getRawY();

                deltaX = event.getRawX() - mOrigX;
                deltaY = event.getRawY() - mOrigY;
                mDragged = mDragged || Math.abs(deltaX) > DRAG_DELTA
                        || Math.abs(deltaY) > DRAG_DELTA;
                if (mDragged) {
                    showDeleteBox();
                }
                break;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsDestroyed = true;
        if (mDraggableIcon != null) {
            removeViewIfAttached(mDraggableIcon);
            mDraggableIcon = null;
        }
        if (mDeleteView != null) {
            removeViewIfAttached(mDeleteView);
            mDeleteView = null;
        }
        if (mAnimationTask != null) {
            mAnimationTask.cancel();
            mAnimationTask = null;
        }
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        if (mThemesObserver != null) {
            getContentResolver().unregisterContentObserver(mThemesObserver);
            mThemesObserver = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mThemeConfig = newConfig.themeConfig;
    }

    @Override
    public void onProgress(int progress) {
    }

    @Override
    public void onFinish(boolean isSuccess) {
        ThemeManager tm = (ThemeManager) getSystemService(Context.THEME_SERVICE);
        tm.removeClient(this);
        mDraggableIconImage.findViewById(R.id.icon).setVisibility(View.VISIBLE);
        mThemeListLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideScrim();
            }
        }, THEME_CHANGE_DELAY);
    }

    public void hideThemeList() {
        hideThemeList(false, new Runnable() {
            @Override
            public void run() {
                removeViewIfAttached(mThemeListLayout);
            }
        });
    }

    private void removeViewIfAttached(View view) {
        if (view.isAttachedToWindow()) {
            mWindowManager.removeViewImmediate(view);
        }
    }

    private WindowManager.LayoutParams addView(View v, int x, int y) {
        return addView(v, x, y, Gravity.TOP | Gravity.LEFT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private WindowManager.LayoutParams addView(View v, int x, int y, int gravity,
            int width, int height) {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = gravity;
        params.x = x;
        params.y = y;

        mWindowManager.addView(v, params);

        return params;
    }

    private void updateIconPosition(int x, int y) {
        mCurrentPosX = x;

        View v = mDraggableIconImage;
        v.setTranslationX(0);
        if (x < 0) {
            v.setTranslationX(x);
            x = 0;
        }

        if (x > getScreenWidth() - FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationX(x - getScreenWidth() + FLOATING_WINDOW_ICON_SIZE);
            x = getScreenWidth() - FLOATING_WINDOW_ICON_SIZE;
        }

        v.setTranslationY(0);
        if (y < 0) {
            v.setTranslationY(y);
            y = 0;
        }

        if (y > getScreenHeight() - FLOATING_WINDOW_ICON_SIZE) {
            v.setTranslationY(y - getScreenHeight() + FLOATING_WINDOW_ICON_SIZE);
            y = getScreenHeight() - FLOATING_WINDOW_ICON_SIZE;
        }
        mParams.x = x;
        mParams.y = y;

        if (!mIsDestroyed) {
            mWindowManager.updateViewLayout(mDraggableIcon, mParams);
        }
    }

    private boolean isDeleteMode() {
        return isDeleteMode(mParams.x, mParams.y);
    }

    private boolean isDeleteMode(int x, int y) {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        int boxWidth = DELETE_BOX_WIDTH;
        int boxHeight = DELETE_BOX_HEIGHT;

        boolean horz = x + (mDraggableIcon == null ? 0
                : mDraggableIcon.getWidth()) > (screenWidth / 2 - boxWidth / 2)
                && x < (screenWidth / 2 + boxWidth / 2);

        boolean vert = y + (mDraggableIcon == null ? 0
                : mDraggableIcon.getHeight()) > (screenHeight - boxHeight);

        return horz && vert;
    }

    private void showDeleteBox() {
        if (!mDeleteBoxVisible) {
            mDeleteBoxVisible = true;
            if (mDeleteView == null) {
                mDeleteView = new LinearLayout(getContext());
                View.inflate(getContext(), R.layout.per_app_delete_box_window, mDeleteView);
                mDeleteBoxView = mDeleteView.findViewById(R.id.box);
                addView(mDeleteView, 0, 0, Gravity.BOTTOM | Gravity.CENTER_VERTICAL,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
            } else {
                mDeleteView.setVisibility(View.VISIBLE);
            }

            mDeleteBoxView.setAlpha(0);
            mDeleteBoxView.setTranslationY(CLOSE_ANIMATION_DISTANCE);
            mDeleteBoxView.animate().alpha(1).translationYBy(-1 * CLOSE_ANIMATION_DISTANCE)
                          .setListener(null);

            mDeleteBoxView.getLayoutParams().width = getScreenWidth();
        }
    }

    private void hideDeleteBox() {
        if (mDeleteBoxVisible) {
            mDeleteBoxVisible = false;
            if (mDeleteView != null) {
                mDeleteBoxView.animate().alpha(0)
                              .translationYBy(CLOSE_ANIMATION_DISTANCE)
                              .setListener(new Animator.AnimatorListener() {
                                  @Override
                                  public void onAnimationStart(Animator animation) {
                                  }

                                  @Override
                                  public void onAnimationEnd(Animator animation) {
                                      if (mDeleteView != null) mDeleteView.setVisibility(View.GONE);
                                  }

                                  @Override
                                  public void onAnimationCancel(Animator animation) {
                                  }

                                  @Override
                                  public void onAnimationRepeat(Animator animation) {
                                  }
                              });
            }
        }
    }

    private void animateToDeleteBoxCenter(final OnAnimationFinishedListener l) {
        if (mIsAnimationLocked) {
            return;
        }
        mIsInDeleteMode = true;

        if (mAnimationTask != null) {
            mAnimationTask.cancel();
        }

        mAnimationTask = new AnimationTask(getScreenWidth() / 2 - mDraggableIcon.getWidth() / 2,
                getScreenHeight() - DELETE_BOX_HEIGHT / 2 - mDraggableIcon.getHeight() / 2);
        mAnimationTask.setDuration(MOVE_TO_DELETE_BOX_ANIMATION_DURATION);
        mAnimationTask.setAnimationFinishedListener(l);
        mAnimationTask.run();
        mDeleteBoxView.setBackgroundResource(R.drawable.btn_quicktheme_remove_hover);
    }

    private void close(boolean animate) {
        if (mIsBeingDestroyed) {
            return;
        }
        mIsBeingDestroyed = true;

        if (animate) {
            animateToDeleteBoxCenter(new OnAnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    hideDeleteBox();
                    mDeleteBoxView.animate()
                                  .scaleX(DELETE_BOX_ANIMATION_SCALE)
                                  .scaleY(DELETE_BOX_ANIMATION_SCALE);
                    mDraggableIconImage.animate()
                                       .scaleX(DELETE_BOX_ANIMATION_SCALE)
                                       .scaleY(DELETE_BOX_ANIMATION_SCALE)
                                       .translationY(CLOSE_ANIMATION_DISTANCE)
                                       .setDuration(mDeleteBoxView.animate().getDuration())
                                       .setListener(new Animator.AnimatorListener() {
                                           @Override
                                           public void onAnimationStart(Animator animation) {
                                           }

                                           @Override
                                           public void onAnimationEnd(Animator animation) {
                                               stopSelf();
                                           }

                                           @Override
                                           public void onAnimationCancel(Animator animation) {
                                           }

                                           @Override
                                           public void onAnimationRepeat(Animator animation) {
                                           }
                                       });
                }
            });
        } else {
            stopSelf();
        }
    }

    private static interface OnAnimationFinishedListener {
        public void onAnimationFinished();
    }

    private Context getContext() {
        return this;
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return getResources().getDisplayMetrics().heightPixels - getStatusBarHeight();
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    private void loadThemes() {
        String[] columns = {ThemesColumns._ID, ThemesColumns.TITLE, ThemesColumns.PKG_NAME};
        String selection = ThemesColumns.MODIFIES_OVERLAYS + "=? AND " +
                ThemesColumns.INSTALL_STATE + "=?";
        String[] selectionArgs = {"1", "" + ThemesColumns.InstallState.INSTALLED};
        String sortOrder = ThemesColumns.TITLE + " ASC";
        Cursor c = getContentResolver().query(ThemesColumns.CONTENT_URI, columns, selection,
                selectionArgs, sortOrder);
        if (c != null) {
            if (mAdapter == null) {
                mAdapter = new ThemesAdapter(this, c);
                mThemeList.setAdapter(mAdapter);
                mThemeList.setOnItemClickListener(mThemeClickedListener);
            } else {
                String pkgName = (String) mAdapter.getItem(0);
                mAdapter.populateThemes(c);
                mAdapter.setCurrentTheme(pkgName);
            }
        }
    }

    private ContentObserver mThemesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            loadThemes();
        }
    };

    private void showThemeList(final int listSide) {
        if (mListLayoutParams == null) {
            mListLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                            WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                    PixelFormat.TRANSLUCENT);
        }
        mListLayoutParams.gravity = Gravity.TOP |
                (listSide == LIST_ON_LEFT_SIDE ? Gravity.LEFT : Gravity.RIGHT);
        mWindowManager.addView(mThemeListLayout, mListLayoutParams);

        mDraggableIconImage.animate()
                           .alpha(0f)
                           .setDuration(ANIMATION_DURATION)
                           .withEndAction(new Runnable() {
                               @Override
                               public void run() {
                                   mDraggableIcon.setVisibility(View.GONE);
                               }
                           });

        setThemeListPosition(listSide);
        mAdapter.setCurrentTheme(
                mThemeConfig.getOverlayPkgNameForApp(Utils.getTopTaskPackageName(this)));
        mThemeListLayout.circularReveal(mParams.x + mDraggableIconImage.getWidth() / 2,
                mParams.y + mDraggableIconImage.getHeight() / 2, ANIMATION_DURATION);
    }

    private void hideThemeList(boolean showScrim, final Runnable endAction) {
        if (showScrim) {
            showScrim();
        } else {
            mDraggableIcon.setVisibility(View.VISIBLE);
            mDraggableIconImage.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION);
        }
        mThemeListLayout.circularHide(mParams.x + mDraggableIconImage.getWidth() / 2,
                mParams.y + mDraggableIconImage.getHeight() / 2, ANIMATION_DURATION);
        if (endAction != null) {
            mDraggableIcon.postDelayed(endAction, ANIMATION_DURATION);
        }
    }

    private void showScrim() {
        ValueAnimator animator = ValueAnimator.ofArgb(SCRIM_COLOR_TRANSPARENT,
                SCRIM_COLOR_OPAQUE);
        mThemeListLayout.setEnabled(false);
        animator.setDuration(ANIMATION_DURATION)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer value = (Integer) animation.getAnimatedValue();
                        mThemeListLayout.setBackgroundColor(value.intValue());
                    }
                });
        animator.start();
        mThemeApplyingView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION);
    }

    private void hideScrim() {
        ValueAnimator animator = ValueAnimator.ofArgb(SCRIM_COLOR_OPAQUE, SCRIM_COLOR_TRANSPARENT);
        animator.setDuration(ANIMATION_DURATION)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer value = (Integer) animation.getAnimatedValue();
                        mThemeListLayout.setBackgroundColor(value.intValue());
                    }
                });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeViewIfAttached(mThemeListLayout);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
        mThemeApplyingView.animate()
                          .alpha(0f)
                .setDuration(ANIMATION_DURATION);
        mDraggableIcon.setVisibility(View.VISIBLE);
        mDraggableIconImage.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION);
    }

    private void setThemeListPosition(final int listSide) {
        int thirdHeight = getScreenHeight() / 3;
        // use the center of the fab to decide where to place the list
        int fabLocationY = mParams.y + mDraggableIconImage.getHeight() / 2;
        int listHeight = mThemeList.getMeasuredHeight();
        if (listHeight <= 0) {
            // not measured yet so let's force that
            int width = getResources().getDimensionPixelSize(R.dimen.theme_list_width);
            int height = getResources().getDimensionPixelSize(R.dimen.theme_list_max_height);
            mThemeList.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
            listHeight = mThemeList.getMeasuredHeight();
        }

        // If we're in the top 1/3 of the screen position the top of the list with the top
        // of the fab.  Second 3rd will position the list so that it is vertically centered
        // with the fab center.  Bottom 3rd will position the bottom of the list with the
        // bottom of the fab.
        if (fabLocationY < thirdHeight) {
            mListParams.topMargin = mParams.y;
        } else if (fabLocationY < thirdHeight * 2) {
            mListParams.topMargin = fabLocationY - listHeight / 2;
        } else {
            mListParams.topMargin = mParams.y + mDraggableIconImage.getHeight() - listHeight;
        }
        mListParams.gravity = Gravity.TOP |
                (listSide == LIST_ON_LEFT_SIDE ? Gravity.LEFT : Gravity.RIGHT);
        mThemeList.setLayoutParams(mListParams);
    }

    private AdapterView.OnItemClickListener mThemeClickedListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final String themePkgName = (String) view.getTag(R.id.tag_key_name);
            final String appPkgName = Utils.getTopTaskPackageName(getContext());
            if (!TextUtils.isEmpty(appPkgName) && !TextUtils.isEmpty(themePkgName)) {
                if (!Utils.themeHasOverlayForApp(getContext(), appPkgName, themePkgName)) {
                    Toast.makeText(getContext(), R.string.per_app_theme_app_not_overlaid_warning,
                            Toast.LENGTH_LONG).show();
                }
                hideThemeList(true, new Runnable() {
                    @Override
                    public void run() {
                        ThemeManager tm = (ThemeManager) getSystemService(Context.THEME_SERVICE);
                        ThemeChangeRequest.Builder builder = new ThemeChangeRequest.Builder();
                        builder.setAppOverlay(appPkgName, themePkgName);
                        tm.addClient(PerAppThemingWindow.this);
                        tm.requestThemeChange(builder.build(), false);
                    }
                });
            } else {
                hideThemeList();
            }
        }
    };

    private float calculateVelocityX() {
        int depreciation = mDeltaXArray.size() + 1;
        float sum = 0;
        for (Float f : mDeltaXArray) {
            depreciation--;
            if (depreciation > MAX_DEPRECIATION){
                continue;
            }

            sum += f / depreciation;
        }

        return sum;
    }

    private float calculateVelocityY() {
        int depreciation = mDeltaYArray.size() + 1;
        float sum = 0;
        for (Float f : mDeltaYArray) {
            depreciation--;
            if (depreciation > 5) {
                continue;
            }

            sum += f / depreciation;
        }

        return sum;
    }

    // Timer for animation/automatic movement of the tray
    private class AnimationTask {
        // Ultimate destination coordinates toward which the view will move
        int mDestX;
        int mDestY;
        long mDuration = 350;
        long mStartTime;
        float mTension = 1.4f;
        Interpolator mInterpolator = new OvershootInterpolator(mTension);
        long mSteps;
        long mCurrentStep;
        int mDistX;
        int mOrigX;
        int mDistY;
        int mOrigY;
        Handler mAnimationHandler = new Handler();
        OnAnimationFinishedListener mAnimationFinishedListener;

        public AnimationTask(int x, int y) {
            setup(x, y);
        }

        public AnimationTask() {
            setup(calculateX(), calculateY());

            float velocityX = calculateVelocityX();
            float velocityY = calculateVelocityY();
            mTension += Math.sqrt(velocityX * velocityX + velocityY * velocityY) / 200;
            mInterpolator = new OvershootInterpolator(mTension);
        }

        private void setup(int x, int y) {
            if (mIsAnimationLocked) {
                throw new RuntimeException("Returning to user's finger. Avoid animations while " +
                        "mIsAnimationLocked flag is set.");
            }

            mDestX = x;
            mDestY = y;

            mSteps = (int) (((float) mDuration) / 1000 * ANIMATION_FRAME_RATE);
            mCurrentStep = 1;
            mDistX = mParams.x - mDestX;
            mOrigX = mParams.x;
            mDistY = mParams.y - mDestY;
            mOrigY = mParams.y;
        }

        public long getDuration() {
            return mDuration;
        }

        public void setDuration(long duration) {
            mDuration = duration;
            setup(mDestX, mDestY);
        }

        public OnAnimationFinishedListener getAnimationFinishedListener() {
            return mAnimationFinishedListener;
        }

        public void setAnimationFinishedListener(OnAnimationFinishedListener l) {
            mAnimationFinishedListener = l;
        }

        public Interpolator getInterpolator() {
            return mInterpolator;
        }

        public void setInterpolator(Interpolator interpolator) {
            mInterpolator = interpolator;
        }

        private int calculateX() {
            float velocityX = calculateVelocityX();
            int screenWidth = getScreenWidth();
            int destX = (mParams.x + mDraggableIcon.getWidth() / 2 > screenWidth / 2)
                        ? screenWidth - mDraggableIcon.getWidth() - MARGIN_HORIZONTAL
                        : 0 + MARGIN_HORIZONTAL;

            if (Math.abs(velocityX) > 50) {
                destX = (velocityX > 0) ? screenWidth - mDraggableIcon.getWidth()
                        - MARGIN_HORIZONTAL : 0 + MARGIN_HORIZONTAL;
            }

            return destX;
        }

        private int calculateY() {
            float velocityY = calculateVelocityY();
            mInterpolator = new OvershootInterpolator(mTension);
            int screenHeight = getScreenHeight();
            int destY = mParams.y + (int) (velocityY * 3);
            if (destY <= 0) {
                destY = MARGIN_VERTICAL;
            }
            if (destY >= screenHeight - mDraggableIcon.getHeight()) {
                destY = screenHeight - mDraggableIcon.getHeight() - MARGIN_VERTICAL;
            }

            return destY;
        }

        public void run() {
            mStartTime = System.currentTimeMillis();
            for (mCurrentStep = 1; mCurrentStep <= mSteps; mCurrentStep++) {
                long delay = mCurrentStep * mDuration / mSteps;
                final float currentStep = mCurrentStep;
                mAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Update coordinates of the view
                        float percent = mInterpolator.getInterpolation(currentStep / mSteps);
                        updateIconPosition(mOrigX - (int) (percent * mDistX), mOrigY
                                - (int) (percent * mDistY));

                        // Notify the animation has ended
                        if (currentStep >= mSteps) {
                            if (mAnimationFinishedListener != null) mAnimationFinishedListener
                                    .onAnimationFinished();
                        }
                    }
                }, delay);
            }
        }

        public void cancel() {
            mAnimationHandler.removeCallbacksAndMessages(null);
            mAnimationTask = null;
        }
    }

    /**
     * We're extending BaseAdapter rather than CursorAdapter so that we can quickly re-order
     * the list without needing to requery the provider.  We're only storing the package name
     * and theme title so there is minimum memory impact on doing this.
     */
    class ThemesAdapter extends BaseAdapter {
        private static final float HALF_OPACITY = 0.5f;
        private static final float FULL_OPACITY = 1.0f;

        private ArrayList<ThemeInfo> mThemes;
        private LayoutInflater mInflater;

        public ThemesAdapter(Context context, Cursor cursor) {
            mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mThemes = new ArrayList<ThemeInfo>(cursor.getCount());
            populateThemes(cursor);
            cursor.close();
        }

        @Override
        public int getCount() {
            return mThemes.size();
        }

        @Override
        public Object getItem(int position) {
            return mThemes.get(position).pkgName;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.per_app_theme_list_item, parent, false);
                Holder holder = new Holder();
                holder.title = (TextView) convertView.findViewById(R.id.theme_title);
                holder.indicator = (TextView) convertView.findViewById(R.id.selected_indicator);
                convertView.setTag(R.id.tag_key_holder, holder);
            }
            ThemeInfo themeInfo = mThemes.get(position);
            Holder holder = (Holder) convertView.getTag(R.id.tag_key_holder);
            holder.title.setText(themeInfo.title);
            if (position == 0) {
                holder.title.setAlpha(HALF_OPACITY);
                holder.indicator.setVisibility(View.VISIBLE);
                convertView.setEnabled(false);
            } else {
                holder.title.setAlpha(FULL_OPACITY);
                holder.indicator.setVisibility(View.INVISIBLE);
                convertView.setEnabled(true);
            }
            convertView.setTag(R.id.tag_key_name, themeInfo.pkgName);
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return position != 0;
        }

        public void setCurrentTheme(String pkgName) {
            ThemeInfo info = null;
            for (ThemeInfo ti : mThemes) {
                if (ti.pkgName.equals(pkgName)) {
                    info = ti;
                    break;
                }
            }
            if (info != null) {
                Collections.sort(mThemes);
                mThemes.remove(info);
                mThemes.add(0, info);
                notifyDataSetChanged();
            }
        }

        private void populateThemes(Cursor cursor) {
            mThemes.clear();
            while(cursor.moveToNext()) {
                ThemeInfo info = new ThemeInfo(
                        cursor.getString(cursor.getColumnIndex(ThemesColumns.PKG_NAME)),
                        cursor.getString(cursor.getColumnIndex(ThemesColumns.TITLE)));
                mThemes.add(info);
            }
        }

        private class Holder {
            TextView title;
            TextView indicator;
        }

        private class ThemeInfo implements Comparable {
            String pkgName;
            String title;

            public ThemeInfo(String pkgName, String title) {
                this.pkgName = pkgName;
                this.title = title;
            }

            @Override
            public int compareTo(Object another) {
                return this.title.compareTo(((ThemeInfo)another).title);
            }
        }
    }
}
