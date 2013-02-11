LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := functions
LOCAL_CFLAGS    := -Werror -fexceptions
LOCAL_SRC_FILES := functions.cpp

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := programmer
LOCAL_CFLAGS    := -fexceptions
LOCAL_SRC_FILES := hexfile_binding.cpp hexfile.cpp chipdefs.cpp chipdefs_binding.cpp

include $(BUILD_SHARED_LIBRARY)
