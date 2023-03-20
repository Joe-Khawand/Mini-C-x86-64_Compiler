struct S {
  int i;
  struct S *b;
};

int main() {
  struct S *s;
  s->i = 0;
  s->b = s;
}
