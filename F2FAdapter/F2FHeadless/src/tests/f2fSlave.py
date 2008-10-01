# start the headless client (slave)
import sys
import os

# make top path available
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))

sys.argv=["headlessclient",
      "localhost", # server
      "f2f02", # username
      "f2f", # password
      "f2fheadless", # resource
      "f2f01@jabber.ulno.net", # list of friends, who can demand resources
      ]

import headlessclient
