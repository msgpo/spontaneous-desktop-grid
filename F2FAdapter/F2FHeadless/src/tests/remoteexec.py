# Try some remote executions

import marshal
import pickle
import threading

Y = 25

testmodule = """
import sys
import pickle

def show_var(v):
    print "Printing var:",v
    
X=23
Y=10

print "X:", X
print "Y:", Y
print "Hello"
"""

print testmodule

#print "Globals, locals:"
#print globals()
#print locals()


print "=== Now executing. ===="
print

#exec( testmodule )
myglobals = {'__builtins__': globals()['__builtins__'],
             '__file__': '/home/ulno/work/eclipse/F2FTNG/F2FHeadless/src/tests/remoteexec.py',
             '__name__': '__main__', '__doc__': None}
#testmodulecomp = compile( testmodule, '<string>', 'exec')
testmodulecomp = compile( testmodule, '<f2f job>', 'exec')
#testmodulecomp.show_var( 5 )
#f=open("marshaltest.bin","w")
#marshal.dump(testmodulecomp,f)
#print testmodulecomp
#exec( testmodulecomp, dict(), dict() )

def runjob(job):
    exec (job)
    
t = threading.Thread(target=runjob,args=(testmodulecomp,)) 

t.start()

print
print "=== Executing end. ===="

#show_var(X)
