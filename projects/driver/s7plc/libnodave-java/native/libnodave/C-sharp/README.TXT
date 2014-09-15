Whoever wants to use this, better look into Dot.NET subdir! 
 
I have been asked about using Libnodave with .NET and to be able to answer, I tried out MONO. 
MONO and .NET are new to me, so the test programs may reflect my weak abilities in this field.
Better solutions from experienced programmers are welcome.

These test programs implement a very simple application that reads from an MPI connection. They
use different ways to access libnodave internal stucture from C#.

test.cs just defines pointers to daveInterface and daveConnection structs. 
Hence, the code is unsafe.

But the member fields of daveInterface and daveConnection are not important to an average 
application. See also nodavesimple.h. This version of libnodave also provides some extra
functions to access the fields in an OO like style, daveGetTimeout, daveSetTimeout etc.
Thus, it is possible to deceive C# about the "real nature" of the pointers and declare them 
as ints. The resulting code is no more "unsafe" in the sense of the compiler...

test2.cs does this.

But now, it is unsafe in another sense because you can put any integer where only the 
"pseudo pointers" are appropriate and you can interchange the "pseudo pointers" of
incompatible types. In one sense, to make your program run well, you allways have to pass back not
only the right type but the right contents, i.e. a pointer initialized pointing at a rightly 
initialized daveInterface or daveConnection, or it will fail. But with type check, the compiler
can find simple things like typos.

test3.cs tries to reestablish type safety.

To do so, it defines private methods dealing with the "pseudo pointers" which are invoked by public
methods defined with wrapper types. The drawbacks are:
1. that the names used in libnodave are occupied by the private methods to match .dll names.
2. The code gets longer.

test4.cs avoids the 1st one using a separate class for the imports

testMPI.cs uses the methods tried out in test4 to implement most features of testMPI.c.

testMPI3.cs tries to pack more functions into classes and to make sure that the memory
malloc'ed in libnodave is:
- allways owned by an instance of a class 
- this memory is free'd again when in the owning class' destructor

I leave it to the .net gurus to decide what is most appropriate.

/*
	I cannot say why, but when I recompiled the existing code with latest libnodave.dll
	(after using stdcall so that VC++ producs these "decorated names", I got a runtime
	error about not finding daveNewInterface. When I state full name entry point explicitly,
	(like below) it runs. The most strange thing is that all other functions work well...
	Maybe this an odd behaviour of MONO and MS .NET users may expirience no or more
	difficulties.
*/
	[DllImport("libnodave.dll", PreserveSig=false, EntryPoint="_daveNewInterface@24")]

Finally, I got rid of decorated names.
This way, the same .exe can be run under Windows and Linux.


