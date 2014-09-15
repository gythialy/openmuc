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

    res = daveReadBytes(dc, daveFlags, 0, 0, 16, NULL);

    if (res == 0) {
      a = daveGetU32(dc);
      printf("FW0 = %08x\n", a);
    }
    else {
      printf("ERROR: Failed to read flags!\n");
    }
  }
  else {
    printf("ERROR: Cannot open tcp socket!\n");
  }
} /* main() */
