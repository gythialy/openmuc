This is a list of all functions Libnodave provides. Those marked with "i" are implemented. Those
marked with "t" also have been tested. daveStringCopy is a special thing implemented for 
interaction with Visual Basic.

The davePut...() functions marked "to do ?" are used in C language to put a value into an array
of unsigned chars which than serves as a buffer for daveWriteBytes() etc. 

i 		char *  daveStrerror(int code);
not implemented	void  daveStringCopy(char * intString, char * extString);
i		void  daveSetDebug(int nDebug);
i		int  daveGetDebug(void);
i		daveInterface *  daveNewInterface(_daveOSserialType nfd, char * nname, int localMPI, int protocol, int speed);
i		daveConnection *  daveNewConnection(daveInterface * di, int MPI,int rack, int slot);
i		int  daveGetResponse(daveConnection * dc);
i		int  daveSendMessage(daveConnection * dc, PDU * p);
		void  _daveDumpPDU(PDU * p);
		void  _daveDump(char * name,uc*b,int len);
		char *  daveBlockName(uc bn);
		char *  daveAreaName(uc n);
		short  daveSwapIed_16(short ff);
		int  daveSwapIed_32(int ff);
		float  daveGetFloatAt(daveConnection * dc, int pos);
		float  toPLCfloat(float ff);
		int  daveToPLCfloat(float ff);

		int  daveGetS8from(uc *b);
		int  daveGetU8from(uc *b);
		int  daveGetS16from(uc *b);
		int  daveGetU16from(uc *b);
		int  daveGetS32from(uc *b);
		unsigned int  daveGetU32from(uc *b);
		float  daveGetFloatfrom(uc *b);

		int  daveGetS8(daveConnection * dc);
		int  daveGetU8(daveConnection * dc);
		int  daveGetS16(daveConnection * dc);
		int  daveGetU16(daveConnection * dc);
		int  daveGetS32(daveConnection * dc);
		unsigned int  daveGetU32(daveConnection * dc);
 		float  daveGetFloat(daveConnection * dc);

		int  daveGetS8At(daveConnection * dc, int pos);
		int  daveGetU8At(daveConnection * dc, int pos);
		int  daveGetS16At(daveConnection * dc, int pos);
		int  daveGetU16At(daveConnection * dc, int pos);
		int  daveGetS32At(daveConnection * dc, int pos);
		unsigned int  daveGetU32At(daveConnection * dc, int pos);

		uc *  davePut8(uc *b,int v);
		uc *  davePut16(uc *b,int v);
		uc *  davePut32(uc *b,int v);
		uc *  davePutFloat(uc *b,float v);

		void  davePut8At(uc *b, int pos, int v);
		void  davePut16At(uc *b, int pos, int v);
		void  davePut32At(uc *b, int pos, int v);
		void  davePutFloatAt(uc *b,int pos, float v);

		float  daveGetSeconds(daveConnection * dc);
		float  daveGetSecondsAt(daveConnection * dc, int pos);

		int  daveGetCounterValue(daveConnection * dc);
		int  daveGetCounterValueAt(daveConnection * dc,int pos);

not implemented	void  _daveConstructUpload(PDU *p,char blockType, int blockNr);
not implemented	void  _daveConstructDoUpload(PDU * p, int uploadID);
not implemented	void  _daveConstructEndUpload(PDU * p, int uploadID);

		int  daveGetOrderCode(daveConnection * dc,char * buf);

		int  daveConnectPLC(daveConnection * dc);
		int  daveReadBytes(daveConnection * dc, int area, int DB, int start, int len, void * buffer);
		int  daveWriteBytes(daveConnection * dc,int area, int DB, int start, int len, void * buffer);
		int  daveReadBits(daveConnection * dc, int area, int DB, int start, int len, void * buffer);
		int  daveWriteBits(daveConnection * dc,int area, int DB, int start, int len, void * buffer);
		int  daveSetBit(daveConnection * dc,int area, int DB, int byteAdr, int bitAdr);
		int  daveClrBit(daveConnection * dc,int area, int DB, int byteAdr, int bitAdr);

		int  daveReadSZL(daveConnection * dc, int ID, int index, void * buf);

		int  daveListBlocksOfType(daveConnection * dc,uc type,daveBlockEntry * buf);
		int  daveListBlocks(daveConnection * dc,daveBlockTypeEntry * buf);

not implemented	int  initUpload(daveConnection * dc,char blockType, int blockNr, int * uploadID);
not implemented	int  doUpload(daveConnection*dc, int * more, uc**buffer, int*len, int uploadID);
not implemented	int  endUpload(daveConnection*dc, int uploadID);

		int  daveStop(daveConnection*dc);
		int  daveStart(daveConnection*dc);
		int  daveForce200(daveConnection * dc, int area, int start, int val);

		void  davePrepareReadRequest(daveConnection * dc, PDU *p);
		void  daveAddVarToReadRequest(PDU *p, int area, int DBnum, int start, int bytes);
		int  daveExecReadRequest(daveConnection * dc, PDU *p, daveResultSet * rl);
		int  daveUseResult(daveConnection * dc, daveResultSet * rl, int n);
		void  daveFreeResults(daveResultSet * rl);
		void  daveAddBitVarToReadRequest(PDU *p, int area, int DBnum, int start, int byteCount);

		void  davePrepareWriteRequest(daveConnection * dc, PDU *p);
		void  daveAddVarToWriteRequest(PDU *p, int area, int DBnum, int start, int bytes, void * buffer);
		void  daveAddBitVarToWriteRequest(PDU *p, int area, int DBnum, int start, int byteCount, void * buffer);
		int   daveExecWriteRequest(daveConnection * dc, PDU *p, daveResultSet * rl);

		int  daveInitAdapter(daveInterface * di);
		int  daveDisconnectPLC(daveConnection * dc);

		int  daveDisconnectAdapter(daveInterface * di);

		int  daveListReachablePartners(daveInterface * di,char * buf);

		void  daveSetTimeout(daveInterface * di, int tmo);
		int  daveGetTimeout(daveInterface * di);

		char *  daveGetName(daveInterface * di);

		int  daveGetMPIAdr(daveConnection * dc);
		int  daveGetAnswLen(daveConnection * dc);
		daveResultSet *  daveNewResultSet();
		void  daveFree(void * dc);
		PDU *  daveNewPDU();
		int  daveGetErrorOfResult(daveResultSet *,int number);
		int  daveForceDisconnectIBH(daveInterface * di, int src, int dest, int mpi);

		int daveGetProgramBlock(daveConnection * dc, int blockType, int number, char* buffer);