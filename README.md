<img src="https://github.com/hashmapinc/hashmap.github.io/blob/master/images/tempus/Tempus_Logo_Black_with_TagLine.png" width="950" height="245" alt="Hashmap, Inc Tempus"/>

[![License](http://img.shields.io/:license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt) [![Build Status](https://travis-ci.org/hashmapinc/nifi-witsml-bundle.svg?branch=master)](https://travis-ci.org/hashmapinc/nifi-witsml-bundle)


# nifi-management-bundle

A NiFi processor bundle for device management which fetches the Operating System and Java Virtaul Machine (JVM) related 
details and write them to flowfile content. It uses `java.lang.management.ManagementFactory` for fetching system and JVM
 related details. By default it only writes the timestamp of the system in JSON format to the flowfile. The nifi-processor
has support for adding dynamic properties to the processor to fetch system and JVM related details.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Usage](#usage)

## Features

This library provide Nifi Processor :
*  GetHearbeat Processor : to fetch current TIMESTAMP of system and dynamically on basis of Processor properties fetch system related details.
*  Supported System Parameters : System CPU Usage, System Memory Usage, System Swap Space Usage, Virtual Memory Committed.
*  Supported JVM Parameters : Heap Usage by JVM, Stack Usage by JVM, Number of live threads, Number of currently loaded classes.

## Requirements

* JDK 1.8 at a minimum
* Maven 3.1 or newer
* Nifi 1.3.0

## Getting Started
To build the library and get started first off clone the GitHub repository 

    git clone https://github.com/hashmapinc/nifi-management-bundle.git

Change directory into the nifi-witsml-bundle

    cd nifi-management-bundle
    
Execute a maven clean install

    mvn clean install
    
A Build success message should appear
      
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 8.944 s
    [INFO] Finished at: 2017-09-15T10:47:18+05:30
    [INFO] Final Memory: 33M/526M
    [INFO] ------------------------------------------------------------------------


A NAR file should be located in the following directory

    {repo_location}/nifi-management-bundle/nifi-management-nar/target
    
Copy this NAR file to the /lib directory and restart (or start) Nifi.

## Usage

#### Finding the GetHeartbeat Processor

Once NiFi is restarted the processors should be able to be added as normal, by dragging a processor onto the NiFi canvas. You can filter the long list of processors by typing heartbeat, you will see a Processor GetHeartbeat.

By default processor will write system timestamp as a heartbeat in JSON format to flowfile as follows :

```json
{ "lastTimeDataReceived" : "2017-09-13T08:03:059Z"}
```
The dynamic properties work on the basis of keywords added in the property name of the processor.

##### For example :

Keywords for fetching details : 
```text
| Property          | PropertyName in Processor | Keyword in Property |
|---------------------------------------------------------------------|
| CPU Usage         | systemCPUUsage            | CPU                 |
| Memory Usage      | systemMemoryUsage         | memory              |
| Heap Usage by JVM | jvmHeapUsage              | heap                |
| StackUsage by JVM | jvmStackUsage             | stack               |
| Swap Space        | swapSpace                 | swap                |
| Virtual Mempry    | virtualMemory             | virtaul             |
| JVM Thread Count  | jvmThreadCount            | thread              |
| JVM Classes Cpunt | jvmClassCount             | class               |
``` 

Output of fetching the above mentioned properties :
```json
{
  "lastTimeDataReceived" : "2017-09-13T08:03:059Z",
  "systemCPUUsage" : 46.42105157150209,
  "systemMemoryUsage" : 15612.34765625,
  "jvmHeapUsage" : 387.97117614746094,
  "jvmStackUsage" : 178.97013092041016,
  "swapSpace" : 528.30859375,
  "virtualMemory" : 15612.34765625,
  "jvmClassCount" : 17111,
  "jvmThreadCount" : 68
}
```

Unit of the data displayed above :
```text
TIMESTAMP  : ISO-8601
CPU Usage  : % (percentage)
All Memory : (KB, MB, GB)
```
Memory Usage unit can be chosen in processor property. Default is in MB, above data is also in MB. 


