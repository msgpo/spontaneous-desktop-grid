#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int fac(int n) {
	if (n <2) return 1;
	return((n)*fac(n-1));
}

char *reverse(char *s){
	register char t,
	*p = s,
	*q = (s + (strlen(s) -1));
	
	while (s && (p < q)){
		t = *p;
		*p++ = *q;
		*q-- = t;
	}
	
	return s;
}

main() {
	//char s[BUFSIZ];
	int index = 0;
	while(index != 10){
		printf ("%d! == %d\n", index, fac(index));
		index++;
	}
	//
	char s[BUFSIZ];
	strcpy(s, "123456");
	printf("reverse \"%s\" -> \"%s\"\n", s, reverse(s));
}
