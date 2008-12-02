# start the headless server (master)
import sys
import os

# make top path available
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))

from headlessclient import f2fHeadless

#f2fHeadless( "localhost", # server
#      "f2f01", # username
#      "f2f", # password
#      "f2fheadless", # resource
#      [], # list of friends (empty)
#      "test computation group", # name of computation group
#      "testjob.py" # submitted job archive
#      )

f2fHeadless( "kheiron.at.mt.ut.ee", # server
      "f2f01", # username
      "f2f", # password
      "f2fheadless", # resource
      5222, # port
      ["f2f02@kheiron.at.mt.ut.ee"], # list of friends
      "test computation group", # name of computation group
      "p2psimple.py" # submitted job archive
      )

#sys.argv=["headlessclient",
#      "localhost", # server
#      "f2f01", # username
#      "f2f", # password
#      "f2fheadless", # resource
#      "f2f02@jabber.ulno.net", # list of friends (only one)
#      "test computation group", # name of computation group
#      "testjob.py" # submitted job archive
#      ]
#
#f = open( os.path.join("..","headlessclient.py") )
#exec(f)
#f.close()