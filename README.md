# OpenMUC Framework - Overview [![Build Status](https://travis-ci.org/gythialy/openmuc.svg?branch=master)](https://travis-ci.org/gythialy/openmuc)

This is UNOFFICIAL, fork from [openmuc](www.openmuc.org/openmuc/).

## OpenMUC
OpenMUC is a software framework based on Java and OSGi that simplifies the development of customized **monitoring, logging and controlling** systems. It can be used as a basis to flexibly implement anything from simple data loggers to complex SCADA systems. The main goal of OpenMUC is to shield the application developer of monitoring and controlling applications from the details of the communication protocol and data logging technologies. Third parties are encouraged to create their own customized systems based on OpenMUC. OpenMUC is licensed under the GPL. We sell individual licenses on request.

## Applications
At Fraunhofer ISE we use the flexible OpenMUC framework as a basis in various smart grid projects. Among other tasks we use it in energy management gateways to readout smart meters, control CHP units, monitor PV systems and control electric vehicle charging. Therefore the OpenMUC framework includes mostly communication protocol drivers from the energy domain. But due to its open and modular architecture there is virtually no limit to the number of applications that can be realized using OpenMUC.

## Features
In summary OpenMUC features the following highlights:

- Easy application development: OpenMUC offers an abstract service for accessing data. Developers can focus on the application's logic rather than the details of the communication and data logging technology.
- Simple and flexible configuration: All communication and data logging parameters can be dynamically configured through a central file or the framework's configuration service.
- Communication support: Out of the box support for several popular communication protocols. New communication protocol drivers can easily be added through a plug-in interface. Existing protocol drivers:
	- Modbus TCP
	- IEC 61850
	- DLMS/COSEM
	- KNX
	- M-Bus (wired)
	- eHz meters
	- CANopen
	- IEC 62056-21
	- S7 PLC protocol
	- SNMP

- Data logging: Data can be logged in two formats (ASCII & binary). New data loggers can easily be added through a plug-in interface.
- Web interface: Convenient user interface for configuration and visualization.
- Data servers: Remote applications (e.g. smart phone apps, cloud applications) or local non-Java applications can access OpenMUC through one of the available data servers (e.g. a RESTful Web Service).
- Modularity: Drivers, data loggers etc. are all individual components. By selecting only the components you need you can create a very light weight system.
- Embedded systems: The framework is designed to run on low-power embedded devices. It is currently being used on embedded x86 and ARM systems. Because OpenMUC is based on Java and OSGi it is platform independent.
- Open-source: The software is being developed at the Fraunhofer Institute for Solar Energy Systems in Freiburg, Germany and is licensed under the GPLv3. We sell individually nagotiated licenses upon request.

## Development & Support
We can support your development. We offer:

- guaranteed support contracts
- custom implementations based on OpenMUC.

## Architecture
The following picture illustrates how OpenMUC works.

![OpenMUC framework overview](http://i.imgur.com/JrMoCv0.png)

OpenMUC framework overview

All boxes seen in the picture are implemented as software modules called OSGi bundles that run independently in the OSGi environment. All modules except for the Data Manager are optional. Thus by selecting the modules you need you can easily create your own customized and light weight system. An explanation of these modules can be found in the user guide.

## Authors
### Developers:

- Stefan Feuerhahn
- Marco Mittelsdorf
- Dirk Zimmermann
- Albrecht Schall
- Philipp Fels

### Former developers:

- Michael Zillgith
- Frederic Robra
- Karsten Müller-Bier

Read the [user guide](https://rawgit.com/gythialy/openmuc/master/doc/userguide/openmuc-doc.html) on how to get started with the library.
