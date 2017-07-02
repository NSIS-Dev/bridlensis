# BridleNSIS

BridleNSIS is a language extension for NSIS (Nullsoft Scriptable Install System) designed to make things easier to express and rein in verbosity of NSIS at places.

Latest version: [0.3.0](https://github.com/NSIS-Dev/bridlensis/releases/download/v0.3.0/BridleNSIS-0.3.0.exe)

## Build From Source Code

In order to build the project you need JDK 1.7 or newer, Apache ANT 1.8 or newer, and NSIS for compiling the installer. BridleNSIS tries to detect the NSIS home directory automatically unless explicitly defined in property `bridle.nsis.home` at `build.properties` file.

1.   Compile BridleNSIS Java classes: `ant compile`
2.   Compile and run JUnit tests: `ant test`
3.   Create distributable jar package: `ant jar`
4.   Generate HTML documents: `ant doc`
5.   Make installer: `ant installer`

Or simply build all by `ant`.
