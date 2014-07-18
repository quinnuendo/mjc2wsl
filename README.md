MJC2WSL - convert MicroJava Compiled Bytecode to WSL/FermaT
===========================================================

mjc2wsl is distributed under the GPL licence, for more details check the src
folder. More about the tools can be found in the "docs" folder.


Quick build and test
--------------------

Running ant in the folder should compile the tool as well as the MJ compiler
and then do a test - compile all of the sample MicroJava codes into MJ
bytecode, translate them to WSL and then apply the given transformations in
MetaWSL to the programs.

Look in the "docs" folder for more details about running the available ant
tasks.


Requirements
------------

- java JDK for compiling and running the mjc2wsl tool and the MJ compiler

- ant for building and testing
-- ant-contrib tasks are needed for some of the tasks

- FermaT for running the translated examples and applying the transformations
(FermaT Maintanance Environment can be used)
Downloads available at http://www.cse.dmu.ac.uk/~mward/fermat.html


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


Contact
-------

Project home:
http://perun.dmi.rs/pracner/transformations

Feel free to contact Doni Pracner if you have any problems, ideas 
or suggestions.