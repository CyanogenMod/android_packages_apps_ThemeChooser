LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

# Obfuscate user builds, eng and userdebug will remain unobfuscated
ifeq ($(TARGET_BUILD_VARIANT),user)
LOCAL_PROGUARD_ENABLED := obfuscation
endif

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_OVERRIDES_PACKAGES := ThemeChooser
LOCAL_PACKAGE_NAME := ModThemeChooser
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    org.cyanogenmod.platform.internal

include $(BUILD_PACKAGE)
