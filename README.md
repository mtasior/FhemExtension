# A bidirectional communication framework for FHEM

## External dependencies

This project uses the following external dependencies (IntelliJ integrated maven notation)

**Google Gson** com.google.code.gson:gson:Latest
**Apache Http Components:** org.apache.httpcomponents:httpclient:Latest
**Java Websocket:** org.java-websocket:Java-WebSocket:Latest

To use them in IntelliJ Idea, just copy the coordinates and add them in
_File -> Project Structure -> Libraries -> + -> From Maven_

## Idea
The general concept behind this framework is: create a Websocket, listen to all messages from FHEM and control dummy devices that represent the "External devices" in the Kotlin by setting the appropriate values into the dummy device.

## Usage

Define all needed values in Constants.kt
run Main.kt.

For every external device you may want to integrate into FHEM, create a class that inherits from `FhemExternalDevice`.
