MJC2WSL - convert MicroJava Compiled Bytecode to WSL/FermaT
===========================================================

mjc2wsl is distributed under the GPL licence, for more details check the src
folder. More about the tools can be found in the "docs" folder.


Quick build and test
--------------------

Running ant in the folder should compile the tool and then do a test -
compile all of the sample MicroJava codes into MJ bytecode, translate them
to WSL and then apply the given transformations in MetaWSL to the programs.

Look in the "docs" folder for more details about running the available ant
tasks, configurations, installations etc.


Requirements
------------

 - java JDK for compiling and running the mjc2wsl tool and the MJ compiler

 - ant for building and testing
      - ant-contrib tasks are needed for some of the tasks

 - FermaT for running the translated examples and applying the transformations
   (FermaT Maintenance Environment can be used)
   Downloads available at http://www.cse.dmu.ac.uk/~mward/fermat.html

Check the docs folder for a more detailed tutorial on how to install
FermaT under Linux.

This project should work correctly regardless of operating
system, that is it should work fine if you have the above
requirements setup properly (java-ant-fermat).

It should be noted that although everything *should* work
on Windows or Mac or whatever else can run the dependencies,
it is only (at this point) extensively tested under Linux.
Feel free to ask about problems running it, suggestions for
improving the platform Independence are welcome.


Setting the FermaT dir
----------------------

The ant build script assumes that Fermat is in "C:/fermat3" under Windows
and "~/fermat3" for everything else (Linux/Mac...).

If you need to change this you can rename (or copy) the given file
"custom.properties.default" to "custom.properties" and specify a correct
path for your system.


About MicroJava
---------------

This is a simple language made by H. Mössenböck for a Compiler Construction
course.  It is NOT the same as Java Micro Edition (JavaME) used on mobile
phones.

For details about the language and the version used in this project check
out the "docs" folder.

More about the course, the language and its VM:
http://www.ssw.uni-linz.ac.at/Misc/CC/

A functional compiler is distributed in the lib folder for ease of testing
and evaluating.  Check the folder for further details.


Git status and mirrors
----------------------

The `master` branch should hold a stable version at all times.  The `work`
branch is usually the current status of the tool, probably stable as well,
but more experimental and untested. There is a possibility of other feature
branches appearing here and there. These are not neccessarily on all mirrors
at the same time. Efforts are made that master is always in sync.

mjc2wsl has a primary git repo on svarog.pmf.uns.ac.rs:

 - http://svarog.pmf.uns.ac.rs/gitweb/?p=mjc2wsl.git

mirrors are also available:

 - https://bitbucket.org/quinnuendo/mjc2wsl

 - https://github.com/quinnuendo/mjc2wsl


Contact
-------

Project home:
http://perun.dmi.rs/pracner/transformations

Feel free to contact Doni Pracner if you have any problems, ideas 
or suggestions.