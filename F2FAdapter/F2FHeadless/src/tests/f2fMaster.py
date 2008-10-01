# start the headless server (master)
import sys
import os

# make top path available
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))

sys.argv=["headlessclient",
      "localhost", # server
      "f2f01", # username
      "f2f", # password
      "f2fheadless", # resource
      "f2f02@jabber.ulno.net", # list of friends (only one)
      "test computation group", # name of computation group
      "testjob" # submitted job archive
      ]

import headlessclient
