/*
	gcc -c example.c -Wall -Winline -DLINUX -DDAVE_LITTLE_ENDIAN
	gcc -o example example.o openSocket.o nodave.o

	oder mit Benutzung der Shared lib libnodave.so:

	gcc -o example example.o -lnodave -L.
	export LD_LIBRARY_PATH=.

 */

#include <stdio.h>

#include "nodave.h"

int
main (int argc, char** argv) {
  daveInterface* di;
  daveConnection* dc;
  _daveOSserialType fds;

  int useSlot = 2;
  int res;
  int a;

  if (argc > 1) {
    printf("arg: %s\n", argv[1]);
  }
  else {
    printf("Usage: example <ip-address>\n");
    exit(1);
  }

  fds.rfd = openSocket (102, argv[1]);
  fds.wfd = fds.rfd;

  if (fds.rfd > 0) {
    di = daveNewInterface(fds, "IF1", 0, daveProtoISOTCP, daveSpeed187k);
    daveSetTimeout(di, 5000000);
    dc = daveNewConnection(di, 2, 0, useSlot);

    if (daveConnectPLC(dc) == 0) {
      printf("Connected to PLC\n");
    }
    else {
      printf("ERROR: Cannot connect to PLC!\n");
      exit(1);
    }

    /* Set PLC to STOP mode */
    // daveStart(dc);
    //

#if 1
    res = daveReadBytes(dc, daveDB, 18, 0, 128, NULL);

    if (res == 0) {
      printf("CHP 1\n---------\n");
      int i;
      float batt_vlt = 0.f;
      float speed = 0.f;
      float return_flow = 0.f;

	      // a = daveGetU32(dc);
      printf("getU16\n");
      i = daveGetU16(dc);
      printf("DB18.0 = %i\n", i);

      batt_vlt = daveGetFloatAt(dc, 28);
      speed = daveGetFloatAt(dc, 36);
      return_flow = daveGetFloatAt(dc, 40);

      printf("Battery voltage: %f\n", batt_vlt);
      printf("Speed: %f\n", speed);
      printf("return temp: %f °C\n", return_flow);

      printf("V_L1: %f V\n", daveGetFloatAt(dc, 56));
      printf("V_L2: %f V\n", daveGetFloatAt(dc, 60));
      printf("V_L3: %f V\n", daveGetFloatAt(dc, 64));

      printf("I_L1: %f A\n", daveGetFloatAt(dc, 80));
      printf("I_L2: %f A\n", daveGetFloatAt(dc, 84));
      printf("I_L3: %f A\n", daveGetFloatAt(dc, 88));

      printf("Power: %f kW\n", daveGetFloatAt(dc, 100));

    }
    else {
      printf("ERROR: Failed to read CHP data!\n");
    }

    res = daveReadBytes(dc, daveDB, 21, 0, 128, NULL);

    if (res == 0) {
      printf("CHP 2\n---------\n");
      int i;
      float batt_vlt = 0.f;
      float speed = 0.f;
      float return_flow = 0.f;

	      // a = daveGetU32(dc);
      printf("getU16\n");
      i = daveGetU16(dc);
      printf("DB18.0 = %i\n", i);

      batt_vlt = daveGetFloatAt(dc, 28);
      speed = daveGetFloatAt(dc, 36);
      return_flow = daveGetFloatAt(dc, 40);

      printf("Battery voltage: %f\n", batt_vlt);
      printf("Speed: %f\n", speed);
      printf("return temp: %f °C\n", return_flow);

      printf("V_L1: %f V\n", daveGetFloatAt(dc, 56));
      printf("V_L2: %f V\n", daveGetFloatAt(dc, 60));
      printf("V_L3: %f V\n", daveGetFloatAt(dc, 64));

      printf("I_L1: %f A\n", daveGetFloatAt(dc, 80));
      printf("I_L2: %f A\n", daveGetFloatAt(dc, 84));
      printf("I_L3: %f A\n", daveGetFloatAt(dc, 88));

      printf("Power: %f kW\n", daveGetFloatAt(dc, 100));
 



    }
    else {
      printf("ERROR: Failed to read CHP data!\n");
    }

    res = daveReadBytes(dc, daveDB, 3, 0, 64, NULL);
    if (res == 0) {
      printf("BOILER\n---------\n");
      printf("Forward temp: %f °C\n", daveGetFloatAt(dc, 28));
      printf("Return temp: %f °C\n", daveGetFloatAt(dc, 32));

      printf("Boiler 1 setpoint: %f\n", daveGetFloatAt(dc, 44));
      printf("Boiler 1 forward temp: %f °C\n", daveGetFloatAt(dc, 8));

      printf("Boiler 2 setpoint: %f\n", daveGetFloatAt(dc, 48));
      printf("Boiler 2 forward temp: %f °C\n", daveGetFloatAt(dc, 12));



    }
    else {
      printf("ERROR: Failed to read boiler data!\n");
    }





#endif


    daveDisconnectPLC(dc);

    
  }
  else {
    printf("ERROR: Cannot open tcp socket!\n");
  }
} /* main() */
