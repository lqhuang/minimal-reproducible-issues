package nested

object Outer {
  object InnerObject {
    def foo() = println("Hello from Inner")
  }

  abstract class JvmCompatibleInnerClass {
    def foo() = println("Hello from Inner")
  }
  object JvmCompatibleInnerClass {
    def foo() = println("Hello from Inner")
  }
}
