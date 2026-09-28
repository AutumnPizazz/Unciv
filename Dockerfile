ARG ARG_COMPILE_BASE_IMAGE=accetto/ubuntu-vnc-xfce-opengl-g3

FROM $ARG_COMPILE_BASE_IMAGE AS build

USER root
RUN  apt update && \
        apt upgrade -y && \
        apt install --fix-broken -y wget curl openjdk-21-jdk unzip

WORKDIR /src
# Get dependencies
RUN wget -q -O packr-all-4.0.0.jar https://github.com/libgdx/packr/releases/download/4.0.0/packr-all-4.0.0.jar && \
          wget -q -O jre-linux-64.tar.gz https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_linux_hotspot_21.0.12.1_1.tar.gz


# Gradle is dumb (https://github.com/gradle/gradle/issues/22921) and doesn't recognize the JDK location
# Solution from https://www.linux.org.ru/forum/desktop/17285826 ¯\_(ツ)_/¯
RUN rm -rf /usr/lib/jvm/openjdk-21
RUN ln -s /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/openjdk-21


# https://nieldw.medium.com/caching-gradle-binaries-in-a-docker-build-when-using-the-gradle-wrapper-277c17e7dd22
# Get gradle distribution
COPY *.gradle gradle.* gradlew /src/
COPY gradle /src/gradle
WORKDIR /src
RUN chmod +x ./gradlew && ./gradlew --version

# Build unciv
COPY . /src/
RUN chmod +x ./gradlew && ./gradlew desktop:classes && \
 ./gradlew desktop:dist && \
 ./gradlew desktop:zipLinuxFilesForJar && \
 ./gradlew desktop:packrLinux64 --stacktrace --info --daemon --scan && \
 cd /src/deploy && unzip UncivCN-Linux64.zip

FROM accetto/ubuntu-vnc-xfce-opengl-g3 AS run
WORKDIR /home/headless/Desktop/
COPY --chown=1001:1001 --from=build /src/deploy/* /usr/
COPY --chown=1001:1001 --from=build /src/desktop/build/libs/UncivCN.jar /usr/share/Unciv/Unciv.jar
COPY --chown=1001:1001 --chmod=0755 --from=build /src/desktop/linuxFilesForJar/* /home/headless/Desktop/
USER 1001
CMD [ "/home/headless/Desktop/Unciv.sh" ]
