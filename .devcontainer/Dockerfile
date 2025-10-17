FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Install required packages
RUN apt-get update && apt-get install -y \
  unzip \
    wget \
      curl \
        git \
          zip \
            lib32stdc++6 \
              lib32z1 \
                libglib2.0-0 \
                  libxcb1 \
                    libx11-6 \
                      libx11-xcb1 \
                        libxrender1 \
                          libxtst6 \
                            libxi6 \
                              && apt-get clean

                              # Set environment variables
                              ENV ANDROID_SDK_ROOT=/opt/android-sdk
                              ENV PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:$PATH
                              