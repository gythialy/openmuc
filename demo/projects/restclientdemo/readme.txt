To build is project use the following command:
$ gradle installApp

If everything succeeded, a new folder named "deploy" is created, the sample 
application can be started by calling deploy/bin/openmuc-demo-restclient on 
linux. Windows user call deploy\bin\openmuc-demo-restclient-demo.bat.

Examples:
========
Read all available channels:
$ ./openmuc-demo-restclient -c 

Read the channel CurrentChannel:
$ ./openmuc-demo-restclient -c <CurrentChannel>

----
For more information refere to the manual:
$ ./openmuc-demo-restclient -m  

