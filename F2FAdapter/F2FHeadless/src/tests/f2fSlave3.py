# start the headless client (slave)
import sys
import os

# make top path available
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))

from headlessclient import f2fHeadless

f2fHeadless( "jabber.ulno.net", # server
      "f2f03", # username
      "f2f", # password
      "f2fheadless", # resource
      25222,
      ["f2f01@jabber.ulno.net"], # list of friends, who can demand resources
      "",
      ""
      )

#sys.argv=["headlessclient",
#      "localhost", # server
#      "f2f02", # username
#      "f2f", # password
#      "f2fheadless", # resource
#      "f2f01@jabber.ulno.net", # list of friends, who can demand resources
#      ]
#
#f = open( os.path.join("..","headlessclient.py") )
#exec(f)
#f.close()

