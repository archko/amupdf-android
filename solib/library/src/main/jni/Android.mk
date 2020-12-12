LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := smart-office-lib
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/smart-office-lib.a

#  gradle clean seems to invoke this for all the supported architectures,
#  even those that we have not asked for.
#  so this makes a missing library non-fatal
#  later if we attempt to link to a missing library, that's still
#  fatal.

ifneq (,$(wildcard $(LOCAL_PATH)/$(LOCAL_SRC_FILES)))

  include $(PREBUILT_STATIC_LIBRARY)

  include $(CLEAR_VARS)
  LOCAL_STATIC_LIBRARIES := smart-office-lib

  LOCAL_MODULE := so
  LOCAL_CFLAGS := -Wall -Werror
  LOCAL_LDLIBS := -ljnigraphics  -llog

  ifdef CLANG_ADDRESS_SANITIZER
    # Options required for address sanitizer builds.
    LOCAL_CFLAGS += -fsanitize=address -fsanitize-recover=address -fno-omit-frame-pointer
    LOCAL_LDFLAGS += -fsanitize=address
    LOCAL_ARM_MODE := arm
  endif

  ifdef SCREENS_ARE_R8G8B8X8
    LOCAL_CFLAGS += -DSCREENS_ARE_R8G8B8X8
  else
  ifdef SCREENS_ARE_B5G6R5
    LOCAL_CFLAGS += -DSCREENS_ARE_B5G6R5
  else
    # Default to B5G6R5
    LOCAL_CFLAGS += -DSCREENS_ARE_B5G6R5
  endif
  endif

  LOCAL_SRC_FILES := \
  android-jni.c \
  android-secure-fs.c

  include $(BUILD_SHARED_LIBRARY)

else
  $(warning no library: '$(LOCAL_SRC_FILES)')
endif
