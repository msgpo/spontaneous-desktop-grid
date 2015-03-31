This is only valid for the old f2f version.
For newer developments look at the documentation at http://f2f.ulno.net/development

# Requirements #

  * jdk-6 (jdk-5 is not possible because we are using couple of methods from jdk-6)
  * Eclipse >= 3.2

> _it is possible that we relax requirements to jdk-5 later_

# SIP Communicator #

## Checkout ##

Checkout SIP Communicator (SC) from the SC SVN repository, [revision 3645](https://code.google.com/p/spontaneous-desktop-grid/source/detail?r=3645).

  * for checkout see [SC SVN page](http://www.sip-communicator.org/index.php/Development/VersionControl)
    1. you need to become observer of SC project (send request via dev.java.net and wait for the project leader confirmation, may take time)
    1. in Eclipse open https://sip-communicator.dev.java.net/svn/sip-communicator SVN repository
    1. checkout trunk (may take up to 10 minutes), [revision 3645](https://code.google.com/p/spontaneous-desktop-grid/source/detail?r=3645)
      * it is suggested you rename it to 'sip-communicator'

## Configure and compile ##

  1. copy `.project` and `.classpath` files from `ide/eclipse` to the project root (hint: use _resource_ perspective)
    * you may need to remove a couple of missing libraries from the build path
    * you may need to fix `ant.jar` location in the build path (use `variable -> ECLIPSE_HOME -> extend`)
  1. remove excessive `jdic_stub.jar`s from the build path (otherwise you may get error with the tray icon)
  1. convert project to PDE project
    * right click on project -> PDE tools -> convert
  1. open `META-INF/MANIFEST.MF` -> runtime and export all _net.java.sip.communicator_ packages
  1. fix any errors
  1. run ant with `build.xml make`
  1. run ant with `build.xml run` (this copies OS-specific bundles to the right place, and starts SIP Communicator)

# F2F #

## checkout ##

  1. checkout java/F2F from the [repository](http://code.google.com/p/spontaneous-desktop-grid/source)

## apply patch to SC project ##

  1. apply the patch file "Sip Communicator patch.txt" as a patch to SC project in Eclipse (project's context menu -> Team -> Apply patch), apply with a fuzziness of 16
  1. build SC

## compile ##

  1. fix any errors
  1. run ant with `build.xml make`

## configure and run ##

  1. run SIP
    * set class to _net.java.sip.communicator.launcher.SIPCommunicator_
    * set VM arguments to (replace `linux` with your operating system)
```
-Dfelix.config.properties=file:../F2F/felix.client.run.properties
-Djava.util.logging.config.file=../F2F/conf/logging.properties
-Dnet.java.sip.communicator.SC_HOME_DIR_LOCATION=../F2F/sip-communicator-profiles
-Dnet.java.sip.communicator.SC_HOME_DIR_NAME=01
-Djava.library.path=lib/native/linux
-Djava.security.policy=file:../F2F/mypolicy.policy
-Djava.rmi.server.codebase=file:sc-bundles/f2fgathererrmi.jar
-Dee.ut.f2f.F2F_PROPERTIES_FILE=../F2F/conf/F2FComputing.properties
-Xmx512m
```