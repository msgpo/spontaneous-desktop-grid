PYVER = 2.5

MODULENAME = f2fcoremodule

PYCLIB = /usr/bin
PYINC  = /usr/include/python$(PYVER)

SRC_DIR   = ../src/native
BUILD_DIR = native

CFLAGS=-fPIC -Wall
LDFLAGS=-fPIC

all: build

init:
	mkdir $(BUILD_DIR)
	
build:
	gcc $(SRC_DIR)/$(MODULENAME).c -g -I$(PYINC) $(CFLAGS) -shared -L$(PYLIB) -lpython$(PYVER) -o $(BUILD_DIR)/$(MODULENAME).so
	
clean:
	rm -rf $(BUILD_DIR)/$(MODULENAME).so $(BUILD_DIR)/core	