
using System;
//using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;


class libno {
    public static readonly int daveProtoMPI=0;	/* MPI for S7 300/400 */    
    public static readonly int daveProtoMPI2 = 1;	/* MPI for S7 300/400, "Andrew's version" */
    public static readonly int daveProtoMPI3 = 2;	/* MPI for S7 300/400, Step 7 Version, not yet implemented */
    public static readonly int daveProtoPPI = 10;	/* PPI for S7 200 */

    public static readonly int daveProtoISOTCP = 122;	/* ISO over TCP */
    public static readonly int daveProtoISOTCP243 = 123;	/* ISO over TCP with CP243 */

    public static readonly int daveProtoMPI_IBH = 223;	/* MPI with IBH NetLink MPI to ethernet gateway */
    public static readonly int daveProtoPPI_IBH = 224;	/* PPI with IBH NetLink PPI to ethernet gateway */

    public static readonly int daveProtoUserTransport = 255;	/* Libnodave will pass the PDUs of S7 Communication to user */
					/* defined call back functions. */

    
    /*
     *    ProfiBus speed constants:
    */
    public static readonly int daveSpeed9k = 0;
    public static readonly int daveSpeed19k =   1;
    public static readonly int daveSpeed187k =   2;
    public static readonly int daveSpeed500k =  3;
    public static readonly int daveSpeed1500k =  4;
    public static readonly int daveSpeed45k =    5;
    public static readonly int daveSpeed93k =   6;
    
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
    
/*
    Some definitions for debugging:
*/
    public static readonly int daveDebugRawRead = 0x01;	/* Show the single bytes received */
    public static readonly int daveDebugSpecialChars = 0x02;	/* Show when special chars are read */
    public static readonly int daveDebugRawWrite = 0x04;	/* Show the single bytes written */
    public static readonly int daveDebugListReachables = 0x08;	/* Show the steps when determine devices in MPI net */
    public static readonly int daveDebugInitAdapter = 0x10;	/* Show the steps when Initilizing the MPI adapter */
    public static readonly int daveDebugConnect = 0x20;	/* Show the steps when connecting a PLC */
    public static readonly int daveDebugPacket = 0x40;
    public static readonly int daveDebugByte = 0x80;
    public static readonly int daveDebugCompare = 0x100;
    public static readonly int daveDebugExchange = 0x200;
    public static readonly int daveDebugPDU = 0x400;	/* debug PDU handling */
    public static readonly int daveDebugUpload = 0x800;	/* debug PDU loading program blocks from PLC */
    public static readonly int daveDebugMPI = 0x1000;
    public static readonly int daveDebugPrintErrors = 0x2000;	/* Print error messages */
    public static readonly int daveDebugPassive = 0x4000;

    public static readonly int daveDebugErrorReporting = 0x8000;


    public static readonly int daveDebugAll = 0xffff;

    public static int  daveMPIReachable = 0x30;
    public static int  daveMPIunused = 0x10;
    public static int  davePartnerListSize = 126;
    
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
    
    public struct PDU {
	public int pointer;
    }

    public struct resultSet {
	public int pointer;
    }
    
    class imports {
//	[DllImport("libnodave.dll", PreserveSig=false)]
/*
	I cannot say why, but when I recompiled the existing code with latest libnodave.dll
	(after using stdcall so that VC++ producs these "decorated names", I got a runtime
	error about not finding daveNewInterface. When I state full name entry point explicitly,
	(like below) it runs. The most strange thing is that all other functions work well...
*/
	[DllImport("libnodave.dll", PreserveSig=false)]
	
	protected  static extern int daveNewInterface(
	    daveOSserialType fd,
	    string name,
	    int localMPI,
	    int  useProto,
	    int speed
        );
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveNewConnection(
	    int di,
	    int MPI,
	    int rack,
	    int slot
	);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveInitAdapter(
	    int di
	);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected  static extern int daveListReachablePartners(
	    int di, byte[] buffer
	);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveDisconnectAdapter(int di);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveConnectPLC(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveDisconnectPLC(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveReadBytes(int dc, int area, int DBnumber, int start, int len, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveWriteBytes(int dc, int area, int DBnumber, int start, int len, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveReadBits(int dc, int area, int DBnumber, int start, int len, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
        protected static extern int daveWriteBits(int dc, int area, int DBnumber, int start, int len, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveSetTimeout(int di, int time);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS32(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU32(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS16(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU16(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS8(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU8(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern float daveGetFloat(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetCounterValue(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern float daveGetSeconds(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS32At(int dc,int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU32At(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS16At(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU16At(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetS8At(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetU8At(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern float daveGetFloatAt(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetCounterValueAt(int dc, int pos);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern float daveGetSecondsAt(int dc, int pos);
	
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetAnswLen(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetMaxPDULen(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveReadSZL(int dc,int id,int index,byte[] ddd);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveForce200(int dc, int start, int val);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveNewPDU();
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int davePrepareReadRequest(int dc, int p);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int davePrepareWriteRequest(int dc, int p);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveAddVarToReadRequest(int p, int area, int DBnum, int start, int bytes);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveAddBitVarToReadRequest(int p, int area, int DBnum, int start, int bytes);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveAddVarToWriteRequest(int p, int area, int DBnum, int start, int bytes, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveAddBitVarToWriteRequest(int p, int area, int DBnum, int start, int bytes, byte[] buffer);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveNewResultSet();
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveExecReadRequest(int dc, int p, int rl);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveExecWriteRequest(int dc, int p, int rl);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveFree(int p);
		
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveStart(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveStop(int dc);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveUseResult(int dc, int rs, int number);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveGetErrorOfResult(int rs, int number);
	
	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern void daveFreeResults(int rs);

	[DllImport("libnodave.dll", PreserveSig=false)]
	protected static extern int daveForceDisconnectIBH(int di, int src, int dest, int MPI);
    }
    
    public static daveInterface daveNewInterface(
        daveOSserialType fd,
	string name,
        int localMPI,
	int  useProto,
        int speed
    ) {
	daveInterface di;
	di.pointer=imports.daveNewInterface(fd, name, localMPI, useProto, speed);
	return di;
    }
    
    public static daveConnection 
    daveNewConnection(daveInterface di, int MPI, int rack, int slot) {
	daveConnection dc;
	dc.pointer=imports.daveNewConnection(di.pointer, MPI, rack, slot);
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
    
    public static int daveInitAdapter(daveInterface di) {
	return imports.daveInitAdapter(di.pointer);
    }

    public static int daveListReachablePartners(daveInterface di, byte[] buffer) {
	return imports.daveListReachablePartners(di.pointer, buffer);
    }
    
    public static int daveDisconnectAdapter(daveInterface di) {
	return imports.daveDisconnectAdapter(di.pointer);
    }
    
    public static int daveForceDisconnectIBH(daveInterface di,int src, int dest, int MPI) {
        return imports.daveForceDisconnectIBH(di.pointer, src, dest, MPI);
    }
    
    public static int daveConnectPLC(daveConnection dc){
	return imports.daveConnectPLC(dc.pointer);
    }
    
    public static int daveDisconnectPLC(daveConnection dc) {
	return imports.daveDisconnectPLC(dc.pointer);
    }
    
    public static int daveReadBytes(daveConnection dc, int area, int DBnumber, int start, int len, byte[] buffer) {
	return imports.daveReadBytes(dc.pointer, area, DBnumber, start, len, buffer);
    }
    
    public static int daveWriteBytes(daveConnection dc, int area, int DBnumber, int start, int len, byte[] buffer) {
	return imports.daveWriteBytes(dc.pointer, area, DBnumber, start, len, buffer);
    }
    
    public static int daveReadBits(daveConnection dc, int area, int DBnumber, int start, int len, byte[] buffer) {
	return imports.daveReadBits(dc.pointer, area, DBnumber, start, len, buffer);
    }
    
    public static int daveWriteBits(daveConnection dc, int area, int DBnumber, int start, int len, byte[] buffer) {
	return imports.daveWriteBits(dc.pointer, area, DBnumber, start, len, buffer);
    }
    
    public static int daveGetS32(daveConnection  dc) {
	return imports.daveGetS32(dc.pointer);
    }
    
    public static int daveGetU32(daveConnection  dc) {
	return imports.daveGetU32(dc.pointer);
    }
    
    public static int daveGetS16(daveConnection  dc) {
	return imports.daveGetS16(dc.pointer);
    }
    
    public static int daveGetU16(daveConnection  dc) {
	return imports.daveGetU16(dc.pointer);
    }
    
    public static int daveGetS8(daveConnection  dc) {
	return imports.daveGetS8(dc.pointer);
    }
    
    public static int daveGetU8(daveConnection  dc) {
	return imports.daveGetU8(dc.pointer);
    }
    
    public static float daveGetFloat(daveConnection  dc) {
	return imports.daveGetFloat(dc.pointer);
    }
    
    public static int daveGetCounterValue(daveConnection  dc) {
	return imports.daveGetCounterValue(dc.pointer);
    }
    
    public static float daveGetSeconds(daveConnection  dc) {
	return imports.daveGetSeconds(dc.pointer);
    }
    
    public static int daveGetS32At(daveConnection dc, int pos) {
	return imports.daveGetS32At(dc.pointer, pos);
    }
    
    public static int daveGetU32At(daveConnection dc, int pos) {
	return imports.daveGetU32At(dc.pointer, pos);
    }
    
    public static int daveGetS16At(daveConnection dc, int pos) {
	return imports.daveGetS16At(dc.pointer, pos);
    }
    
    public static int daveGetU16At(daveConnection dc, int pos) {
	return imports.daveGetU16At(dc.pointer, pos);
    }
    
    public static int daveGetS8At(daveConnection dc, int pos) {
	return imports.daveGetS8At(dc.pointer, pos);
    }
    
    public static int daveGetU8At(daveConnection dc, int pos) {
	return imports.daveGetU8At(dc.pointer, pos);
    }
    
    public static float daveGetFloatAt(daveConnection dc, int pos) {
	return imports.daveGetFloatAt(dc.pointer, pos);
    }
    
    public static int daveGetCounterValueAt(daveConnection dc, int pos) {
	return imports.daveGetCounterValueAt(dc.pointer, pos);
    }
    
    public static float daveGetSecondsAt(daveConnection dc, int pos) {
	return imports.daveGetSecondsAt(dc.pointer, pos);
    }
        
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern string 
    daveStrerror(int res);
    
    public static void daveSetTimeout(daveInterface di, int time) {
	imports.daveSetTimeout(di.pointer, time);
    }
    
    public static int daveGetAnswLen(daveConnection dc) {
	return imports.daveGetAnswLen(dc.pointer);
    }
    
    public static int daveGetMaxPDULen(daveConnection dc) {
	return imports.daveGetMaxPDULen(dc.pointer);
    }	

    public static int daveReadSZL(daveConnection dc,int id,int index,byte[] ddd) {
	return imports.daveReadSZL(dc.pointer,id,index, ddd);
    }	
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern float toPLCfloat(float f);
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int daveToPLCfloat(float f);
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int daveSwapIed_32(int i);
    
    [DllImport("libnodave.dll", PreserveSig=false)]
    public static extern int daveSwapIed_16(int i);
    
    public static int daveGetU16from(byte[] b,int pos) {
        return 0x100*b[pos]+b[pos+1];
    }
	
    public static int daveForce200(daveConnection dc, int start, int val) {
	return imports.daveForce200(dc.pointer, start, val);
    }
    
    public static PDU daveNewPDU() {
	PDU p;
	p.pointer=imports.daveNewPDU();
	return p;
    }
    
    public static void davePrepareReadRequest(daveConnection dc, PDU p) {
	imports.davePrepareReadRequest(dc.pointer, p.pointer);
    }
    
    public static void davePrepareWriteRequest(daveConnection dc,PDU p) {
	imports.davePrepareWriteRequest(dc.pointer, p.pointer);
    }
    
    public static void daveAddVarToReadRequest(PDU p, int area, int DBnum, int start, int bytes) {
	imports.daveAddVarToReadRequest(p.pointer, area, DBnum, start, bytes);
    }
    
    public static void daveAddBitVarToReadRequest(PDU p, int area, int DBnum, int start, int bytes) {
	imports.daveAddBitVarToReadRequest(p.pointer, area, DBnum, start, bytes);
    }
    
    public static void daveAddVarToWriteRequest(PDU p, int area, int DBnum, int start, int bytes, byte[] buffer) {
	imports.daveAddVarToWriteRequest(p.pointer, area, DBnum, start, bytes, buffer);
    }
    
    public static void daveAddBitVarToWriteRequest(PDU p, int area, int DBnum, int start, int bytes, byte[] buffer) {
	imports.daveAddBitVarToWriteRequest(p.pointer, area, DBnum, start, bytes, buffer);
    }
    
    public static resultSet daveNewResultSet() {
	resultSet p;
	p.pointer=imports.daveNewResultSet();
	return p;
    }
    
    public static int daveFree(resultSet rs) {
	return imports.daveFree(rs.pointer);
    }
    
    public static int daveFree(daveConnection dc) {
	return imports.daveFree(dc.pointer);
    }
    
    public static int daveFree(PDU rs) {
	return imports.daveFree(rs.pointer);
    }
    

    public static int daveExecReadRequest(daveConnection dc, PDU p, resultSet rl) {
	return imports.daveExecReadRequest(
	    dc.pointer, 
	    p.pointer, 
	    rl.pointer);
    }
    
    public static int daveExecWriteRequest(daveConnection dc, PDU p, resultSet rl) {
	return imports.daveExecWriteRequest(
	    dc.pointer, 
	    p.pointer, 
	    rl.pointer);
    }
    
    public static int daveStart(daveConnection dc) {
	return imports.daveStart(dc.pointer);
    }
    
    public static int daveStop(daveConnection dc) {
	return imports.daveStop(dc.pointer);
    }
    
    public static int daveUseResult(daveConnection dc, resultSet rs, int number) {
	return imports.daveUseResult(dc.pointer, rs.pointer, number);
    }
    
    public static int daveGetErrorOfResult(resultSet rs, int number) {
	return imports.daveGetErrorOfResult(rs.pointer, number);
    }	
    
    public static void daveFreeResults(resultSet rs) {
	imports.daveFreeResults(rs.pointer);
    }
    
}

class test {
    static void wait() {
    }
    
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
    static int plcMPI=2;
    static int plc2MPI=-1;
    static int adrPos=0;	
    static int useProto=libno.daveProtoMPI;
    static int speed=libno.daveSpeed187k;
    static libno.daveOSserialType fds;
    static libno.daveInterface di;
    static libno.daveConnection dc;
    static bool doWrite=false;
    static bool doClear=false;
    static bool doRun=false;
    static bool doStop=false;
    static bool doWbit=false;
    static int wbit;
    static bool doSZLread=false;
    static bool doSZLreadAll=false;
    static bool doBenchmark=false;
    static bool doReadout=false;
    static bool doSFBandSFC=false;
    static bool doExperimental=false;
    static bool doMultiple=false;
    static bool doNewfunctions=false;
    
    static void readSZL(libno.daveConnection dc, int id, int index) {
	int res, SZLid, indx, SZcount, SZlen,i,j,len;
	byte[] ddd=new byte[3000];
        Console.WriteLine(String.Format("Trying to read SZL-ID {0:X04}, index {1:X02}",id,index));
	res=libno.daveReadSZL(dc,id,index, ddd);
        Console.WriteLine("Function result: "+res+" "+libno.daveStrerror(res)+" len:"+libno.daveGetAnswLen(dc));
    
        if (libno.daveGetAnswLen(dc)>=4) {
	    len=libno.daveGetAnswLen(dc)-8;
	    SZLid=libno.daveGetU16from(ddd,0); 
	    indx=libno.daveGetU16from(ddd,2);
	    Console.WriteLine(String.Format("result SZL ID {0:X04}, index {1:X02}",SZLid,indx));
	    int d=8;
	    if (libno.daveGetAnswLen(dc)>=8) {
    		SZlen=libno.daveGetU16from(ddd,4);
    		SZcount=libno.daveGetU16from(ddd,6);
		Console.WriteLine(" "+SZcount+" elements of "+SZlen+" bytes");
		if(len>0){
		    for (i=0;i<SZcount;i++){
			if(len>0){
			    for (j=0; j<SZlen; j++){
				if(len>0){
				    Console.Write(String.Format("{0:X02},",ddd[d]));
				    d++;
				}
				len--;
			    }
			    Console.WriteLine(" ");
			}
		    }
		}
	    }
	}
	Console.WriteLine(" ");
    }    

    static void readSZLAll(libno.daveConnection dc) {
	byte[] d=new byte[1000];
	int res, SZLid, indx, SZcount, SZlen,i,j, rid, rind;
        
	res=libno.daveReadSZL(dc,0,0, d);
        Console.WriteLine(" "+res+" "+libno.daveGetAnswLen(dc));
	if ((libno.daveGetAnswLen(dc))>=4) {
	    SZLid=libno.daveGetU16(dc);
	    indx=libno.daveGetU16(dc);
	    Console.WriteLine(String.Format("result SZL ID {0:X04} index {1:X02}",SZLid,indx));
	    if ((libno.daveGetAnswLen(dc))>=8) {
    	        SZlen=0x100*d[4]+d[5]; 
	        SZcount=0x100*d[6]+d[7]; 
		Console.WriteLine("%d elements of %d bytes\n",SZcount,SZlen);
		for (i=0;i<SZcount;i++){
		    rid=libno.daveGetU16from(d,i*SZlen+8);
		    rind=0;
		    Console.WriteLine(String.Format("\nID:{0:X04} index {1:X02}",rid,rind));
		    readSZL(dc, rid, rind);
		}
	    }
	}
	Console.WriteLine("\n");
    }


    public static int Main (string[] args)
    {
	int i,a=0,j,res,b=0,c=0;
	float d=0;
	byte[] buf1=new byte[libno.davePartnerListSize];

	if (args.Length <1) {
	    usage();
	    return -1;
	}
		
	while (args[adrPos][0]=='-') {
	if (args[adrPos].StartsWith("--debug=")) {
	    libno.daveSetDebug(Convert.ToInt32(args[adrPos].Substring(8)));
	    Console.WriteLine("setting debug to: ",Convert.ToInt32(args[adrPos].Substring(8)));
	} else if (args[adrPos].StartsWith("-d")) {
	    libno.daveSetDebug(libno.daveDebugAll);
	} else if (args[adrPos].StartsWith("-s")) {
	    doStop=true;
	} else if (args[adrPos].StartsWith("-w")) {
	    doWrite=true;
	} else if (args[adrPos].StartsWith("-b")) {
	    doBenchmark=true;
	} else if (args[adrPos].StartsWith("--readoutall")) {
	    doReadout=true;
	    doSFBandSFC=true;
	} else if (args[adrPos].StartsWith("--readout")) {
	    doReadout=true;
	} else if (args[adrPos].StartsWith("-r")) {
	    doRun=true;
	} else if (args[adrPos].StartsWith("-e")) {
	    doExperimental=true;
	} else if (args[adrPos].StartsWith("--local=")) {
	    localMPI=Convert.ToInt32(args[adrPos].Substring(8));
	    Console.WriteLine("setting local MPI address to: "+localMPI);
	} else if (args[adrPos].StartsWith("--mpi=")) {
	    plcMPI=Convert.ToInt32(args[adrPos].Substring(6));
	    Console.WriteLine("setting MPI address of PLC to: "+plcMPI);
	} else if (args[adrPos].StartsWith("--mpi2=")) {
	    plc2MPI=Convert.ToInt32(args[adrPos].Substring(7));
	    Console.WriteLine("setting MPI address of 2md PLC to: "+plc2MPI);
	} else if (args[adrPos].StartsWith("--wbit=")) {
	    wbit=Convert.ToInt32(args[adrPos].Substring(7));
	    Console.WriteLine("setting bit number: "+wbit);
	    doWbit=true;
	} else if (args[adrPos].StartsWith("-z")) {
	    doSZLread=true;
	} else if (args[adrPos].StartsWith("-a")) {
	    doSZLreadAll=true;
	} else if (args[adrPos].StartsWith("-m")) {
	    doMultiple=true;
	} else if (args[adrPos].StartsWith("-c")) {
	    doClear=true;
	} else if (args[adrPos].StartsWith("-n")) {
	    doNewfunctions=true;
	} else if (args[adrPos].StartsWith("-2")) {
	    useProto=libno.daveProtoMPI2;
	} else if (args[adrPos].StartsWith("-3")) {
	    useProto=libno.daveProtoMPI3;
	} else if (args[adrPos].StartsWith("-9")) {
 	    speed=libno.daveSpeed9k;
 	} else if (args[adrPos].StartsWith("-19")) {
 	    speed=libno.daveSpeed19k;
 	} else if (args[adrPos].StartsWith("-45")) {
 	    speed=libno.daveSpeed45k;
 	} else if (args[adrPos].StartsWith("-93")) {
 	    speed=libno.daveSpeed93k;
 	} else if (args[adrPos].StartsWith("-500")) {
 	    speed=libno.daveSpeed500k;
 	} else if (args[adrPos].StartsWith("-1500")) {
 	    speed=libno.daveSpeed1500k;
 	} 
 	
	adrPos++;
	if (args.Length<=adrPos) {
	    usage();
	    return -1;
	}	
	}    

	
        fds.rfd=libno.setPort(args[adrPos],"38400",'O');
	fds.wfd=fds.rfd;
        if (fds.rfd>0) { 
	    di =libno.daveNewInterface(fds, "IF1", localMPI, useProto, speed);
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
	    
	    dc = libno.daveNewConnection(di,plcMPI,0,0);
	    
	    if (0==libno.daveConnectPLC(dc)) {;
		res=libno.daveReadBytes(dc, libno.daveFlags, 0, 0, 16, null);
		if (res==0) {
    		    a=libno.daveGetS32(dc);	
    		    b=libno.daveGetS32(dc);
    		    c=libno.daveGetS32(dc);
		    d=libno.daveGetFloat(dc);
		    Console.WriteLine("FD0: " + a);
		    Console.WriteLine("FD4: " + b);
		    Console.WriteLine("FD8: " + c);
		    Console.WriteLine("FD12: " + d);
		} else 
		    Console.WriteLine("error "+res+" "+libno.daveStrerror(res));
		
		if(doExperimental) {
		    Console.WriteLine("Trying to read outputs");
		    res=libno.daveReadBytes(dc, libno.daveOutputs, 0, 0, 2, null);
		    Console.WriteLine("function result: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    if (res==0) {	
	    		Console.Write("Bytes:");
			for (b=0; b<libno.daveGetAnswLen(dc); b++) {
			    c=libno.daveGetU8(dc);
			    Console.Write(String.Format(" {0:X0}, ",c));
			}
			Console.WriteLine("");
		    }    
		    a=0x01;
		    Console.WriteLine("Trying to write outputs");
		    res=libno.daveWriteBytes(dc, libno.daveOutputs, 0, 0, 1, BitConverter.GetBytes(a));
		    Console.WriteLine("function result: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    libno.daveSetDebug(libno.daveDebugAll);
		    res=libno.daveForce200(dc, 0,0);
		    Console.WriteLine("function result: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    libno.daveSetDebug(0);
		    res=libno.daveForce200(dc, 0,1);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
	    
		    res=libno.daveForce200(dc, 0,2);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
		    res=libno.daveForce200(dc, 0,3);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
		    res=libno.daveForce200(dc, 1,4);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
		    res=libno.daveForce200(dc, 2,5);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
		    res=libno.daveForce200(dc, 3,7);
		    Console.WriteLine("function result of force: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    wait();
		    Console.WriteLine("Trying to read outputs again\n");
		    res=libno.daveReadBytes(dc, libno.daveOutputs, 0, 0, 4, null);
		    Console.WriteLine("function result: "+res+"="+libno.daveStrerror(res)+" "+libno.daveGetAnswLen(dc));
		    if (res==0) {	
	    		Console.Write("Bytes:");
			for (b=0; b<libno.daveGetAnswLen(dc); b++) {
			    c=libno.daveGetU8(dc);
			    Console.Write(String.Format(" {0:X0}, ",c));
			}
			Console.WriteLine("");
		    }    
		}
    
		    
		if(doWrite) {
    		    Console.WriteLine("Now we write back these data after incrementing the integers by 1,2 and 3 and the float by 1.1.\n");
    		    wait();
/*
    Attention! you need to daveSwapIed little endian variables before using them as a buffer for
    daveWriteBytes() or before copying them into a buffer for daveWriteBytes()!
*/	    
        	    a=libno.daveSwapIed_32(a+1);
		    libno.daveWriteBytes(dc,libno.daveFlags,0,0,4,BitConverter.GetBytes(a));
    	    	    b=libno.daveSwapIed_32(b+2);
		    libno.daveWriteBytes(dc,libno.daveFlags,0,4,4,BitConverter.GetBytes(b));
        	    c=libno.daveSwapIed_32(c+3);
		    libno.daveWriteBytes(dc,libno.daveFlags,0,8,4,BitConverter.GetBytes(c));
    	    	    d=libno.toPLCfloat(d+1.1f);
    		    libno.daveWriteBytes(dc,libno.daveFlags,0,12,4,BitConverter.GetBytes(d));
/*
 *   Read back and show the new values, so users may notice the difference:
 */	    
        	    libno.daveReadBytes(dc,libno.daveFlags,0,0,16, null);
		    a=libno.daveGetU32(dc);
    	    	    b=libno.daveGetU32(dc);
		    c=libno.daveGetU32(dc);
        	    d=libno.daveGetFloat(dc);
		    Console.WriteLine("FD0: "+a);
		    Console.WriteLine("FD4: "+b);
		    Console.WriteLine("FD8: "+c);
		    Console.WriteLine("FD12: "+d);
		} // doWrite
		
		if(doClear) {
	    	    Console.WriteLine("Now writing 0 to the bytes FB0...FB15.\n");
//    		    wait();
		    byte[] aa={0,0,0,0};
    		    libno.daveWriteBytes(dc,libno.daveFlags,0,0,4,aa);
        	    libno.daveWriteBytes(dc,libno.daveFlags,0,4,4,aa);
		    libno.daveWriteBytes(dc,libno.daveFlags,0,8,4,aa);
    		    libno.daveWriteBytes(dc,libno.daveFlags,0,12,4,aa);
		    libno.daveReadBytes(dc,libno.daveFlags,0,0,16, null);
    		    a=libno.daveGetU32(dc);
	    	    b=libno.daveGetU32(dc);
    		    c=libno.daveGetU32(dc);
	    	    d=libno.daveGetFloat(dc);
		    Console.WriteLine("FD0: "+a);
		    Console.WriteLine("FD4: "+b);
		    Console.WriteLine("FD8: "+c);
		    Console.WriteLine("FD12: "+d);
		} // doClear
    
		if(doSZLread) {
		    readSZL(dc,0x92,0x0);
	    	    readSZL(dc,0xB4,0x1024);
		    readSZL(dc,0x111,0x1);
		    readSZL(dc,0xD91,0x0);
		    readSZL(dc,0x232,0x4);
		    readSZL(dc,0x1A0,0x0);
		    readSZL(dc,0x0A0,0x0);
		}
		
		if(doSZLreadAll) {
		    readSZLAll(dc);
		}
		
		if(doStop) {
		    libno.daveStop(dc);
		}
		if(doRun) {
		    libno.daveStart(dc);
		}
		
		if(doNewfunctions) {
		    int saveDebug=libno.daveGetDebug();
	    
		    Console.WriteLine("Trying to read two consecutive bits from DB11.DBX0.1");;
		    res=libno.daveReadBits(dc, libno.daveDB, 11, 1, 2, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	    
		    Console.WriteLine("Trying to read no bit (length 0) from DB17.DBX0.1");
		    res=libno.daveReadBits(dc, libno.daveDB, 17, 1, 0, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));

		    libno.daveSetDebug(libno.daveGetDebug()|libno.daveDebugPDU);	
		    Console.WriteLine("Trying to read a single bit from DB17.DBX0.3\n");
		    res=libno.daveReadBits(dc, libno.daveDB, 17, 3, 1, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	
		    Console.WriteLine("Trying to read a single bit from E0.2\n");
		    res=libno.daveReadBits(dc, libno.daveInputs, 0, 2, 1, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	    
		    a=0;
		    Console.WriteLine("Writing 0 to EB0\n");
		    res=libno.daveWriteBytes(dc, libno.daveOutputs, 0, 0, 1, BitConverter.GetBytes(libno.daveSwapIed_32(a)));
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));

		    a=1;
		    Console.WriteLine("Trying to set single bit E0.5\n");
		    res=libno.daveWriteBits(dc, libno.daveOutputs, 0, 5, 1, BitConverter.GetBytes(libno.daveSwapIed_32(a)));
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	
		    Console.WriteLine("Trying to read 1 byte from AAW0\n");
		    res=libno.daveReadBytes(dc, libno.daveAnaIn, 0, 0, 2, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	    
		    a=2341;
		    Console.WriteLine("Trying to write 1 word (2 bytes) to AAW0\n");
		    res=libno.daveWriteBytes(dc, libno.daveAnaOut, 0, 0, 2, BitConverter.GetBytes(libno.daveSwapIed_32(a)));
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
	
		    Console.WriteLine("Trying to read 4 items from Timers\n");
		    res=libno.daveReadBytes(dc, libno.daveTimer, 0, 0, 4, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
		    if(res==0) {
			d=libno.daveGetSeconds(dc);
			Console.WriteLine("Time: %0.3f, ",d);
		        d=libno.daveGetSeconds(dc);
			Console.WriteLine("%0.3f, ",d);
			d=libno.daveGetSeconds(dc);
			Console.WriteLine("%0.3f, ",d);
			d=libno.daveGetSeconds(dc);
			Console.WriteLine(" %0.3f\n",d);
	    
		        d=libno.daveGetSecondsAt(dc,0);
		        Console.WriteLine("Time: %0.3f, ",d);
		        d=libno.daveGetSecondsAt(dc,2);
			Console.WriteLine("%0.3f, ",d);
		        d=libno.daveGetSecondsAt(dc,4);
		        Console.WriteLine("%0.3f, ",d);
		        d=libno.daveGetSecondsAt(dc,6);
		        Console.WriteLine(" %0.3f\n",d);
		    }
		    
		    Console.WriteLine("Trying to read 4 items from Counters\n");
		    res=libno.daveReadBytes(dc, libno.daveCounter, 0, 0, 4, null);
		    Console.WriteLine("function result:" + res+ "="+libno.daveStrerror(res));
		    if(res==0) {
		        c=libno.daveGetCounterValue(dc);
		        Console.WriteLine("Count: %d, ",c);
			c=libno.daveGetCounterValue(dc);
		        Console.WriteLine("%d, ",c);
			c=libno.daveGetCounterValue(dc);
			Console.WriteLine("%d, ",c);
			c=libno.daveGetCounterValue(dc);
	    		Console.WriteLine(" %d\n",c);
	    
			c=libno.daveGetCounterValueAt(dc,0);
		        Console.WriteLine("Count: %d, ",c);
		        c=libno.daveGetCounterValueAt(dc,2);
		        Console.WriteLine("%d, ",c);
		        c=libno.daveGetCounterValueAt(dc,4);
		        Console.WriteLine("%d, ",c);
		        c=libno.daveGetCounterValueAt(dc,6);
		        Console.WriteLine(" %d\n",c);
		    }	
	    
		    libno.PDU p=libno.daveNewPDU();
		    libno.davePrepareReadRequest(dc, p);
		    libno.daveAddVarToReadRequest(p, libno.daveInputs,0,0,1);
		    libno.daveAddVarToReadRequest(p, libno.daveFlags,0,0,4);
		    libno.daveAddVarToReadRequest(p, libno.daveDB,6,20,2);
		    libno.daveAddVarToReadRequest(p, libno.daveTimer,0,0,4);
		    libno.daveAddVarToReadRequest(p, libno.daveTimer,0,1,4);
		    libno.daveAddVarToReadRequest(p, libno.daveTimer,0,2,4);
		    libno.daveAddVarToReadRequest(p, libno.daveCounter,0,0,4);
		    libno.daveAddVarToReadRequest(p, libno.daveCounter,0,1,4);
		    libno.daveAddVarToReadRequest(p, libno.daveCounter,0,2,4);
		    libno.resultSet rs=libno.daveNewResultSet();
	    	    res=libno.daveExecReadRequest(dc, p, rs);
		    libno.daveSetDebug(saveDebug);
		    libno.daveFree(rs);
		    libno.daveFree(p);
		}
		
		if(doMultiple) {
    		    Console.WriteLine("Now testing read multiple variables.\n"
				    +"This will read 1 Byte from inputs,\n"
				    +"4 bytes from flags, 2 bytes from DB6,\n"
				    +"and other 2 bytes from flags");
    		    wait();
//		    libno.davePrepareReadRequest(dc, &p);
		    libno.PDU p=libno.daveNewPDU();
		    Console.WriteLine("here.");
		    libno.davePrepareReadRequest(dc, p);
		    Console.WriteLine("here.2");
		    libno.daveAddVarToReadRequest(p,libno.daveInputs,0,0,1);
		    libno.daveAddVarToReadRequest(p, libno.daveFlags,0,0,4);
		    libno.daveAddVarToReadRequest(p,libno.daveDB,6,20,2);
		    libno.daveAddVarToReadRequest(p,libno.daveFlags,0,12,2);
		    libno.daveAddBitVarToReadRequest(p, libno.daveFlags, 0, 25 /* 25 is 3.1*/, 1);
		    libno.resultSet rs=libno.daveNewResultSet();
		    res=libno.daveExecReadRequest(dc, p, rs);
	    
		    Console.WriteLine("Input Byte 0 ");
		    res=libno.daveUseResult(dc, rs, 0); // first result
		    Console.WriteLine("here.3");
		    if (res==0) {
			a=libno.daveGetU8(dc);
        		Console.WriteLine(a);
		    } else 
			Console.WriteLine("*** Error: "+libno.daveStrerror(res));
		
		    Console.WriteLine("Flag DWord 0 ");	
		    res=libno.daveUseResult(dc, rs, 1); // 2nd result
		    if (res==0) {
			a=libno.daveGetS16(dc);
        		Console.WriteLine(a);
		    } else 
			Console.WriteLine("*** Error: "+libno.daveStrerror(res));
		
		    Console.WriteLine("DB 6 Word 20: ");	
		    res=libno.daveUseResult(dc, rs, 2); // 3rd result
		    if (res==0) {
			a=libno.daveGetS16(dc);
	        	Console.WriteLine(a);
		    } else 
			Console.WriteLine("*** Error: "+libno.daveStrerror(res));
		
		    Console.WriteLine("Flag Word 12: ");		
		    res=libno.daveUseResult(dc, rs, 3); // 4th result
		    if (res==0) {
			a=libno.daveGetU16(dc);
        		Console.WriteLine(a);
	    	    } else 
		    	Console.WriteLine("*** Error: "+libno.daveStrerror(res));	
	    
		    Console.WriteLine("Flag F3.1: ");		
		    res=libno.daveUseResult(dc, rs, 4); // 4th result
		    if (res==0) {
			a=libno.daveGetU8(dc);
	        	Console.WriteLine(a);
		    } else 
			Console.WriteLine("*** Error: "+libno.daveStrerror(res));		
		
		    Console.WriteLine("non existing result (we read 5 items, but try to use a 6th one): ");
		    res=libno.daveUseResult(dc, rs, 5); // 5th result
		    if (res==0) {
			a=libno.daveGetU16(dc);
        		Console.WriteLine(a);
		    } else 
			Console.WriteLine("*** Error: "+libno.daveStrerror(res));		
	    
		    libno.daveFreeResults(rs);  
	    
		    if (doWrite){
			Console.WriteLine("Now testing write multiple variables:\n"
			    +"IB0, FW0, QB0, DB6:DBW20 and DB20:DBD24 in a single multiple write.");
			wait();
//			libno.daveSetDebug(0xffff);
			byte[] aa={0};
	        	libno.davePrepareWriteRequest(dc, p);
			libno.daveAddVarToWriteRequest(p, libno.daveInputs,0,0,1, aa);
			libno.daveAddVarToWriteRequest(p,libno.daveFlags,0,4,2, aa);
	    		libno.daveAddVarToWriteRequest(p,libno.daveOutputs,0,0,2, aa);
			libno.daveAddVarToWriteRequest(p,libno.daveDB,6,20,2, aa);
			libno.daveAddVarToWriteRequest(p,libno.daveDB,20,24,4, aa);
			aa[0]=1;
			libno.daveAddBitVarToWriteRequest(p, libno.daveFlags, 0, 27 /* 27 is 3.3*/, 1, aa);
			res=libno.daveExecWriteRequest(dc, p, rs);
			Console.WriteLine("Result code for the entire multiple write operation: "+res+"="+libno.daveStrerror(res));
/*
//	I could list the single result codes like this, but I want to tell
//	which item should have been written, so I do it in 5 individual lines:
	
			for (i=0;i<rs.numResults;i++){
			    res=rs.results[i].error;
			    Console.WriteLine("result code from writing item %d: %d=%s\n",i,res,libno.libno.daveStrerror(res));
			}
*/		
			int err=libno.daveGetErrorOfResult(rs,0);
			Console.WriteLine("Result code for writing IB0:        "+err+"="+libno.daveStrerror(err));
			err=libno.daveGetErrorOfResult(rs,1);
			Console.WriteLine("Result code for writing FW4:        "+err+"="+libno.daveStrerror(err));
			err=libno.daveGetErrorOfResult(rs,2);
			Console.WriteLine("Result code for writing QB0:        "+err+"="+libno.daveStrerror(err));
			err=libno.daveGetErrorOfResult(rs,3);
			Console.WriteLine("Result code for writing DB6:DBW20:  "+err+"="+libno.daveStrerror(err));
			err=libno.daveGetErrorOfResult(rs,4);
			Console.WriteLine("Result code for writing DB20:DBD24: "+err+"="+libno.daveStrerror(err));
			err=libno.daveGetErrorOfResult(rs,5);
			Console.WriteLine("Result code for writing F3.3:       "+err+"="+libno.daveStrerror(err));
/*
 *   Read back and show the new values, so users may notice the difference:
 */	    
	    		libno.daveReadBytes(dc,libno.daveFlags,0,0,16, null);
    			a=libno.daveGetU32(dc);
	    		b=libno.daveGetU32(dc);
    			c=libno.daveGetU32(dc);
    			d=libno.daveGetFloat(dc);
	    		Console.WriteLine("FD0: %d\n",a);
			Console.WriteLine("FD4: %d\n",b);
			Console.WriteLine("FD8: %d\n",c);
	    		Console.WriteLine("FD12: %f\n",d);		
		    } // doWrite
		}	    

    
		libno.daveDisconnectPLC(dc);    
	    }	    
	    libno.daveDisconnectAdapter(di);
	    di =libno.daveNewInterface(fds, "IF1", localMPI, useProto, speed);
	    for (i=0;i<1000;i++) {
		di =libno.daveNewInterface(fds, "IF1", localMPI, useProto, speed);
		Console.WriteLine("Couldn't open i "+i);
	    }	
	} else {
	    Console.WriteLine("Couldn't open serial port "+args[adrPos]);
	    return -1;
	}	
	return 0;
    }
}
