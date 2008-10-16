print "Hello world f2f! (1)"

import sys
import os
sys.path.insert(1, os.path.realpath(
            os.path.join( sys.path[0], ".." )))

print "Hello world f2f! (2)"

import f2fdfg

print "Hello world f2f! (3)"

#print f2f.myGroup()

# print "Initializing master/slave functions."
