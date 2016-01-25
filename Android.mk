LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := ThemeChooser
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res) \
    $(TOP)/frameworks/support/v7/cardview/res

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    org.cyanogenmod.platform.internal \
    android-support-v7-palette \
    android-support-v7-cardview

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.cardview

include $(BUILD_PACKAGE)
