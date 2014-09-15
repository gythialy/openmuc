
using System;
//using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;


class libno {
    public static int daveProtoMPI=0;	/* MPI for S7 300/400 */    
    
    /*
     *    ProfiBus speed constants:
    */
    public static int daveSpeed9k = 0;
    public static int daveSpeed19k =   1;
    public static int daveSpeed187k =   2;
    public static int daveSpeed500k =  3;
    public static int daveSpeed1500k =  4;
    public static int daveSpeed45k =    5;
    public static int daveSpeed93k =   6;
    
/*
    Use these constants for parameter "area" in daveReadBytes and daveWriteBytes
*/    
    public static readonly int daveSysInfo = 0x3;		/* System info of 200 family */
    public static readonly int daveSysFlags =  0x5;	/* System flags of 200 family */
    public static readonly int daveAnaIn =  0x6;		/* analog inputs of 200 family */
    public static readonly int daveAnaOut =  0x7;		/* analog outputs of 200 family */
    public static readonly int daveP = 0x80;    
    public static readonly int daveInputs = 0x81;   
    public static readonly int daveOutputs = 0x82;    
    public static readonly int daveFlags = 0x83;
    public static readonly int daveDB = 0x84;	/* data blocks */
    public static readonly int daveDI = 0x85;	/* instance data blocks */
    public static readonly int daveLocal = 0x86; 	/* not tested */
    public static readonly int daveV = 0x87;	/* don't know what it is */
    public static readonly int daveCounter = 28;	/* S7 counters */
    public static readonly int daveTimer = 29;	/* S7 timers */
    public static readonly int daveCounter200 = 30;	/* IEC counters (200 family) */
    public static readonly int daveTimer200 = 31;	/* IEC timers (200 family) */



    public static int  daveMPIReachable = 0x30;
    public static int  daveMPIunused = 0x10;
    public static int  davePartnerListSize = 126;

    
//    [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
    [StructLayout(LayoutKind.Sequential)]
    public struct daveOSserialType {
	public int rfd;
	public int wfd;
    }
        

    public struct daveInterface {
	public int pointer;
    }
    
    
    public struct daveConnection {
	public int pointer;
    }
    
//	[DllImport("libnodave.dll", PreserveSig=false)]
/*
	I cannot say why, but when I recompiled the existing code with latest libnodave.dll
	(after using stdcall so that VC++ producs these "decorated names", I got a runtime
	error about not finding daveNewInterface. When I state full name entry point explicitly,
	(like below) it runs. The most strange thing is that all other functions work well...
*/
	[DllImport("libnodave.dll", PreserveSig=false)]
    
//    public struct daveInterface ref
    public static extern int
    daveNewInterface(
//    int di,
    daveOSserialType fd,
//    [MarshalAs(UnmanagedType.LPWStr)] 
    string name,
    int localMPI,
    int  useProto,
    int speed
    );
    
    [DllImport("libnodave.dll", PreserveSig=false)]
//    public static unsafe extern daveConnection *
//    public static extern int
    static extern int
    daveNewConnection(
    int di,
    int MPI,
    int rack,
    int slot
    );
    
    public static daveConnection 
    DaveNewConnection(
    int di,
    int MPI,
    int rack,
    int slot
    ) {
	daveConnection dc;
	dc.pointer=daveNewConnection(
	di,
	MPI,
	rack,
	slot
    );
    return dc;
    }
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern void daveSetDebug(int newDebugLevel);
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int daveGetDebug();
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int setPort(
	[MarshalAs(UnmanagedType.LPStr)] string portName,
	[MarshalAs(UnmanagedType.LPStr)] string baud,
	int parity
    );
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int
    daveInitAdapter(
        int di
//	[MarshalAs(UnmanagedType.LPArray)] daveInterface[] di
    );

    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int 
    daveListReachablePartners(
	int di,
	byte[] buffer
    );
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int 
    daveDisconnectAdapter(int di);

    [DllImport("libnodave.dll", PreserveSig=false)]
    static extern int 
    daveConnectPLC(int dc);
    
    public static int 
    DaveConnectPLC(daveConnection dc){
	return daveConnectPLC(dc.pointer);
    }
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    static extern int 
    daveDisconnectPLC(int dc);
    
    public static int 
    DaveDisconnectPLC(daveConnection dc) {
	return daveDisconnectPLC(dc.pointer);
    }
    
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    static extern int 
    daveReadBytes(int dc, int area, int DBnumber, int start, int len,
	byte[] buffer
    );
    
    public static int 
    DaveReadBytes(daveConnection dc, int area, int DBnumber, int start, int len, byte[] buffer) {
	return daveReadBytes(dc.pointer, area, DBnumber, start, len, buffer);
    }
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    static extern int 
    daveGetS32(int dc);
    
    public static int 
    DaveGetS32(daveConnection  dc) {
	return daveGetS32(dc.pointer);
    }
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    static extern float 
    daveGetFloat(int dc);
    
    public static float 
    DaveGetFloat(daveConnection  dc) {
	return daveGetFloat(dc.pointer);
    }
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern string 
    daveStrerror(int res);
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern void 
    daveSetTimeout(int di, int time);
}

class test {
    static void usage() {
    Console.WriteLine("Usage: testMPI [-d] [-w] serial port.");
    Console.WriteLine("-w will try to write to Flag words. It will overwrite FB0 to FB15 (MB0 to MB15) !");
    Console.WriteLine("-d will produce a lot of debug messages.");
    Console.WriteLine("-b will run benchmarks. Specify -b and -w to run write benchmarks.");
    Console.WriteLine("-z will read some SZL list items (diagnostic information).");
    Console.WriteLine("-2 uses a slightly different version of the MPI protocol. Try it, if your Adapter doesn't work.");
    Console.WriteLine("-m will run a test for multiple variable reads.");
    Console.WriteLine("-c will write 0 to the PLC memory used in write tests.");
    Console.WriteLine("-n will test newly added functions.");
    Console.WriteLine("-a will read out everything from system state lists(SZLs).");
    Console.WriteLine("-s stops the PLC.");
    Console.WriteLine("-r tries to put the PLC in run mode.");
    Console.WriteLine("--readout read program and data blocks from PLC.");
    Console.WriteLine("--readoutall read all program and data blocks from PLC. Includes SFBs and SFCs.");
    Console.WriteLine("-<number> will set the speed of MPI/PROFIBUS network to this value (in kBaud).  Default is 187.  Supported values are 9, 19, 45, 93, 187, 500 and 1500.");
    Console.WriteLine("--mpi=<number> will use number as the MPI adddres of the PLC. Default is 2.");
    Console.WriteLine("--mpi2=<number> Use this option to test simultaneous connections to 2 PLCs."); 
    Console.WriteLine("  It will use number as the MPI adddres of the 2nd PLC. Default is no 2nd PLC.");
    Console.WriteLine("  Most tests are executed with the first PLC. The first read test is also done with");
    Console.WriteLine("  with the 2nd one to demonstrate that this works.");
    Console.WriteLine("--local=<number> will set the local MPI adddres to number. Default is 0.");
    Console.WriteLine("--debug=<number> will set daveDebug to number.");
//#ifdef UNIX_STYLE
	Console.WriteLine("Example: testMPI -w /dev/ttyS0");
//#endif    
//#ifdef WIN_STYLE    
	Console.WriteLine("Example: testMPI -w COM1");
//#endif    
    }

    static int initSuccess=0;
    static int localMPI=0;
    static int plcMPI=4;
    static int adrPos=0;	
    static int useProto=libno.daveProtoMPI;
    static int speed=libno.daveSpeed187k;
    static libno.daveOSserialType fds;
//    static int di;
//    unsafe static libno.daveInterface* di;
    static int di;
    static libno.daveConnection dc;

    public static int Main (string[] args)
    {
	int i,a,j,res,b,c;
	float d;
	byte[] buf1=new byte[libno.davePartnerListSize];

	if (args.Length <1) {
	    usage();
	    return -1;
	}
	fds.rfd=libno.setPort(args[adrPos],"38400",'O');
	fds.wfd=fds.rfd;
	if (fds.rfd>0) { 
	    di =libno.daveNewInterface(fds, "IF1", localMPI, useProto, speed);
	    libno.daveSetTimeout(di,5000000);
	    for (i=0; i<3; i++) {
		if (0==libno.daveInitAdapter(di)) {
		    initSuccess=1;	
		    a= libno.daveListReachablePartners(di,buf1);
		    Console.WriteLine("daveListReachablePartners List length: "+a);
		    if (a>0) {
			for (j=0;j<a;j++) {
			    if (buf1[j]==libno.daveMPIReachable) Console.WriteLine("Device at address: "+j);
			}	
		    }
		    break;	
		} else libno.daveDisconnectAdapter(di);
	    }
	    if (initSuccess==0) {
	        Console.WriteLine("Couldn't connect to Adapter!.\n Please try again. You may also try the option -2 for some adapters.");	
	        return -3;
	    }
	    
	    dc = libno.DaveNewConnection(di,plcMPI,0,0);
	    
	    if (0==libno.DaveConnectPLC(dc)) {;
		res=libno.DaveReadBytes(dc, libno.daveFlags, 0, 0, 16, null);
		if (res==0) {
    		    a=libno.DaveGetS32(dc);	
    		    b=libno.DaveGetS32(dc);
    		    c=libno.DaveGetS32(dc);
		    d=libno.DaveGetFloat(dc);
		    Console.WriteLine("PLC FD0: " + a);
		    Console.WriteLine("PLC FD4: " + b);
		    Console.WriteLine("PLC FD8: " + c);
		    Console.WriteLine("PLC FD12: " + d);
		} else 
		    Console.WriteLine("error "+res+" "+libno.daveStrerror(res));
		libno.DaveDisconnectPLC(dc);    
	    }	    
	    libno.daveDisconnectAdapter(di);
	} else {
	    Console.WriteLine("Couldn't open serial port "+args[adrPos]);
	    return -1;
	}	
	return 0;
    }
}
