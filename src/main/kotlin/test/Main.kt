package test

import org.springframework.core.ResolvableType
import kotlin.reflect.jvm.javaMethod

fun main() {
    val javaMethod = Foo::b_method2.javaMethod
    if (javaMethod == null) {
        println("javaMethod is null")
        return
    }
    val returnType = ResolvableType.forMethodReturnType(javaMethod)
    println(returnType)
}

