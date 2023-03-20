struct S { int a; };
int main() { int x; { struct S *x; x->a = 42; } x = 1; }
