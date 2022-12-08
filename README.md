# Reflection in Kotlin in native-image

## Reflection metadata for method is missing

```kotlin
val javaMethod = Foo::a_method1.javaMethod
```

will fail with

```
Exception in thread "main" kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Could not compute caller for function: public final fun a_method1(): test.A? defined in test.Foo[DeserializedSimpleFunctionDescriptor@1da402a9] (member = null)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:88)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.internal.ReflectProperties$LazyVal.invoke(ReflectProperties.java:63)
        at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:32)
        at kotlin.reflect.jvm.internal.KFunctionImpl.getCaller(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.ReflectJvmMapping.getJavaMethod(ReflectJvmMapping.kt:63)
        at test.MainKt.main(Main.kt:10)
        at test.MainKt.main(Main.kt)
```

if reflection metadata for `Foo.a_method1` is missing.

## Reflection metadata for some methods in the class are missing

The `Foo` class is defined as follows:

```kotlin
class Foo {
    fun a_method1(): A? {
        return null
    }

    fun b_method2(): B? {
        return null
    }
}

interface A

interface B
```

Now let's try some reflection:

```kotlin
val javaMethod = Foo::a_method1.javaMethod
ResolvableType.forMethodReturnType(javaMethod)
```

will work if there is reflection metadata for `Foo.a_method1` but not for `Foo.b_method2`.

```kotlin
val javaMethod = Foo::b_method2.javaMethod
ResolvableType.forMethodReturnType(javaMethod)
```

will not work if there is reflection metadata for `Foo.b_method2` but not for `Foo.a_method1`.

The reason is that Kotlin iterates over the methods until it finds the matching one. `a_method1` will be iterated first
(I assume because of alphabetical sorting), it then tries to inspect it (no idea why, we didn't ask for it) and then it
will fail with:

```
Exception in thread "main" java.lang.ClassNotFoundException: test.A
        at java.base@17.0.5/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:52)
        at java.base@17.0.5/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
        at java.base@17.0.5/java.lang.ClassLoader.loadClass(ClassLoader.java:132)
        at kotlin.reflect.jvm.internal.KDeclarationContainerImpl.parseType(KDeclarationContainerImpl.kt:273)
        at kotlin.reflect.jvm.internal.KDeclarationContainerImpl.loadReturnType(KDeclarationContainerImpl.kt:288)
        at kotlin.reflect.jvm.internal.KDeclarationContainerImpl.findMethodBySignature(KDeclarationContainerImpl.kt:198)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:68)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.internal.ReflectProperties$LazyVal.invoke(ReflectProperties.java:63)
        at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:32)
        at kotlin.reflect.jvm.internal.KFunctionImpl.getCaller(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.ReflectJvmMapping.getJavaMethod(ReflectJvmMapping.kt:63)
        at kotlin.reflect.jvm.ReflectJvmMapping.getKotlinFunction(ReflectJvmMapping.kt:136)
        at org.springframework.core.MethodParameter$KotlinDelegate.getGenericReturnType(MethodParameter.java:914)
        at org.springframework.core.MethodParameter.getGenericParameterType(MethodParameter.java:510)
        at org.springframework.core.SerializableTypeWrapper$MethodParameterTypeProvider.getType(SerializableTypeWrapper.java:291)
        at org.springframework.core.SerializableTypeWrapper.forTypeProvider(SerializableTypeWrapper.java:107)
        at org.springframework.core.ResolvableType.forType(ResolvableType.java:1413)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1334)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1316)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1283)
        at org.springframework.core.ResolvableType.forMethodReturnType(ResolvableType.java:1228)
        at test.MainKt.main(Main.kt:12)
        at test.MainKt.main(Main.kt)
```

`test.A` is the return type of `a_method1` which is not in the native image.

If metadata is added for `test.A`, it fails with a different exception:

```
Exception in thread "main" kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Could not compute caller for function: public final fun a_method1(): test.A? defined in test.Foo[DeserializedSimpleFunctionDescriptor@33119311] (member = null)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:88)
        at kotlin.reflect.jvm.internal.KFunctionImpl$caller$2.invoke(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.internal.ReflectProperties$LazyVal.invoke(ReflectProperties.java:63)
        at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:32)
        at kotlin.reflect.jvm.internal.KFunctionImpl.getCaller(KFunctionImpl.kt:61)
        at kotlin.reflect.jvm.ReflectJvmMapping.getJavaMethod(ReflectJvmMapping.kt:63)
        at kotlin.reflect.jvm.ReflectJvmMapping.getKotlinFunction(ReflectJvmMapping.kt:136)
        at org.springframework.core.MethodParameter$KotlinDelegate.getGenericReturnType(MethodParameter.java:914)
        at org.springframework.core.MethodParameter.getGenericParameterType(MethodParameter.java:510)
        at org.springframework.core.SerializableTypeWrapper$MethodParameterTypeProvider.getType(SerializableTypeWrapper.java:291)
        at org.springframework.core.SerializableTypeWrapper.forTypeProvider(SerializableTypeWrapper.java:107)
        at org.springframework.core.ResolvableType.forType(ResolvableType.java:1413)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1334)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1316)
        at org.springframework.core.ResolvableType.forMethodParameter(ResolvableType.java:1283)
        at org.springframework.core.ResolvableType.forMethodReturnType(ResolvableType.java:1228)
        at test.MainKt.main(Main.kt:12)
        at test.MainKt.main(Main.kt)
```

If both methods have metadata, everything is fine.
