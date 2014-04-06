
Basic Instructions:

I'm going to assume you know how to compile Java software, and have an
environment set up for it.

You can either put all files into a folder in your classpath named
net/sf/sphaero, and compile them:

  /myclasspath/net/sf/sphaero> javac *.java

Or copy them to any folder, and compile them to a desired class path:

  /mysourcefiles> javac -d /myclasspath *.java

If you just want to try out the software, the easiest way is to run
JigsawFrontEnd.  I recommend extra heap space if you're going to use an
image of, say, 800x600 or larger pixels:

  /> java -Xmx256m net.sf.sphaero.JigsawFrontEnd



Future Improvement:

If you want to help with the project, the bug that's caused me the most
grief is the memory bug.  See JigsawFrame.java for a description.  Other
improvements include

- better piece cutting - the classic cutter sometimes leaves "pinhole" gaps
  between pieces (you'll know them when you see them)
- implementing completely free rotation
- a web applet version
- more JigsawCutters!


Enjoy!

- Paul Brinkley (paulbrinkley@sourceforge.net) 20031229 23:02

