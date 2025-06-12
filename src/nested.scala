package nested

object Outer {
  object Inner {
    def foo() = println("Hello from Inner")
  }
}
