#  Ambiguous KDoc references

**Preamble:** The aim of the document is to address the current issues with ambiguous KDoc references that Dokka experiences during the migration to K2 (Analysis API). However, a chosen solution can be applied for future use cases as well.

# Introduction

There are two types of KDoc references to declaration:
-   Fully qualified ones, for example `[com.example.classA]`
-   Relative ones, for example `[memberProperty]`

Also, KDoc allows to refer to:
-   Parameters `[p]`.  They can be no only in `@param [p]` or `@param p`.
-   A receiver via `[this]`

Here is an example for understating:
```kotlin
package com.example
/**  
* [com.example.classA], [com.example.classA.member] - fully qualified references
* [classA], [member], [extension] - relative reference
*/
class classA {
	/**  
	* [classA], [member], [extension] - relative reference
	*/
	val member = 0
}
  
/**  
 * [com.example.extension] - fully qualified reference 
 * [classA], [extension] - relative references in the current scope
 * [classA.extension], [com.example.classA.extension] - out of the scope of the current document
 * [this] - receiver 
 * [p] - parameter 
 * 
 * @param p is also a reference  
 */
 fun classA.extension(p: Int) = 0
```  
*Note: This document does not consider the case of extensions (references to extensions)  for the sake of simplicity.  It is a non-goal and can deserve another dedicated document.*


# Problem

For relative references, there some are cases when KDoc references are ambiguous that means there more than one possible candidate from the users point of view. These cases was discovered by the migration Dokka to K2 and behave differently in K1 and K2. ([original issue](https://github.com/Kotlin/dokka/issues/3451))

### I Reference to itself
Javadoc and KDoc allow to have references to itself. It is a quite spread practice.
However it can also lead to ambiguous reference:
```kotlin
 /**
 *  [A] In K1, it leads to the nested class A.A. In K2 - to the outer class A
 */
class A {
	class A
}
```
The case (a nested class with the same name as enclosing class) can be unpopular since it is banned in Java, C#.

There is a more practical case in Kotlin with a factory function :
```kotlin
/** [A] */
class A
/** [A] */
fun A(p: Int) = A()
```
In K1, the both reference lead to the class A. In K2, they lead to itself.
This case can be applied to all possible pairs of declaration kinds (function and function also known as overloads problem , property and class...).

Also, a constructor has a name of class.
```kotlin
class A    {
	/** 
	* [A] In K1, it leads to the class A. In K2 - to the constructor 
	*/
	constructor(s: String)
}
```
For Javadoc, see the section `Other languages`.

### II References to parameters

This case seems valid.
```kotlin
val abc: String = ""
/**
* [abc] to the parameter in K1 and K2.
* For the property, a fully qulified reference can be used.
*/
fun f(abc: String) = 0
```


However, in a primary constructor, the same reference can refer to parameters and properties. It does not matter for IDE. Opposite,  Dokka has different locations for parameters and properties.
```kotlin
/**
* [abc] K1 refers to the property `abc`, K2 - to the parameter
*/
class A(val abc: String)
```
From the point of IDE view, the link `[abc]` leads to the same position in a source file independently of whether the `abc` is a parameter or property.

#### Related problem: Availability/Visibility of parameters
The availability of parameters inside a scope can result in ambiguous references.
```kotlin
class A(a: Int) {  
	/**  
	* [a] is unresolved in K1. In K2, it is resolved
	*/  
  fun usage() = 0
}
```

## Ambiguity in other cases

Also, there other cases that can be considered as ambiguous, but their behavior is consistent in K1 and K2.

For trivial cases (ambiguous references are inside a single scope) there are the predefined priorities of KDoc candidates in the Dokka and IDE K1 implementations:
-   class
-   package
-   function
-   property

For example,
```kotlin
val x = 0
fun x() = 0

/** here [x] refers to the function */
```
These priorities allow to avoid ambiguity in fully qualified references except the case `I  Reference to itself` above.

In the case of overloads, a KDoc reference leads to the first occurrence.
For example,
```kotlin
fun x(p: int) = 0
fun x() = 0

/** here [x] refers to the function x(p: int) */
```

### III Order of scopes

Currently, an inner scope already has a priority over outer scopes.
Let's consider the the following general example to understand the current resolve of KDoc reference :
```kotlin
class B

/** [B] - K1, Javadoc and K2 refer to the nested class A.B */
class A {
	class B
}
```
The search for the declaration is initially done in the members. Therefore, K1 (IDE and Dokka), K2, and Javadoc refer to a nested class B.

Here is another example:
```kotlin
val a = 0
fun a() = 0

/** [a] K1 and K2 refer to the parameter */
fun f(a: Int) = 0
```

#### Related problem: Availability/Visibility of  nested classes from base classes
However, "inherited" nested classes are in question:
```kotlin
open class DateBased {
	class DayBased
}

/**
* [DayBased] K2 and Javadoc lead to [DateBased.DayBased], but in K1, it is unresolved
*/
class MonthBased : DateBased()
```
This causes inconsistent behavior. In K1, the reference `[B]`  is unresolved, but it resolves inherited members.

It is a problem of determining what declarations the current context of KDoc contains.  Together with that, undefined name resolution for a referred declaration causes ambiguous references.  
*(In other words,  undefined order of scopes in which the search for a referred declaration should be done, see https://kotlinlang.org/spec/scopes-and-identifiers.html#scopes-and-identifiers)*


## Proposals
This section considers **2 solutions**. It is unnecessary to choose only one for all cases above.

*Choosing  a solution can be related to the future support of  KDoc references to overloads and a possible process of deprecation of the old KDoc links.*

### 1 By tooling
The problem of  ambiguous KDoc references can be solved by tooling (Dokka and IDE).
Dokka can show all possible candidates *via a popup with an interactive list* in the same way as IDE does it for ambiguous resolving.  Under the hood, the Analysis API returns a list of KDoc candidates.
![example](https://i.ibb.co/dKQkshh/image.png)
**Pros:**
* it seems suitable for overloads

**Cons:**
* possible irrelevant candidates, for example
  ```kotlin
  fun A = 0
  class A
  /**
  The class [A]..., but it also shows the irrelevant fun
  */
  ```
  or star-imports can contains unexpected declarations with the same name

* might be difficult to implement in HTML. It requires a drop-down list.

### 2 By defining  a set of rules
The solutions is based on creating a set of rules for each particular case.

Also, there is  the rule of thumb for KDoc references that states:
*References should behave as if they were written in code. (omitting some details)*  *(The exception is visibility)*
That means that the context of KDoc reference should contain all available names at a given point in code.
For example, in the documentation of functions/properties - the KDoc context can correspond to a function body.  
This rule can be applied to the **Case III**.

```kotlin
open class C {
	class B
}
class B
/**
* [B] refers to [C.B]
*/
class A : C()
```
The inherited nested class should be available here.


#### Case I
In this case, the rule of thumb for KDoc does not help since all names are available in code. So there 2 options here:
1. Reference to itself should hide other available declarations.
```kotlin
/**
* [A] should refer to itself
*/
class A {
	/**
	* [A] should refer to itself
	*/
	class A
	/**
	* [A] should refer to itself
	* to have a reference to a class, 
	* the fully qualified reference [package.A.A] can be used, according to the priorities from the Introduction section
	*/
	fun A(a: Int)
}
```
**Pros/Cons**  ???
2. Left the behavior of K1, i.e. reuse the priorities from the Introduction section:
-   class
-   package
-   function
-   property

For example,
```kotlin
	/**
	* [A] should refer to class A
	*/
	class A
	/**
	* [A] should refer to to class A
	*/
	fun A(a: Int)
```
**Cons:**
- in this example, there is no way to refer to the function `A`  (itself)

#### Case II
1. Parameters of current declaration should hide other declaration.  It is according to the rule of thumb for KDoc references when, for the documentation of  classes/interfaces... , the KDoc corresponds to an `init` block.
   The visibility of parameters inside a class body is still questionable.
```kotlin
/**
* [abc] should refer to the parameter
* [A.abc] should refer to the property
*/
class A(val abc: String)
```
**Pros/Cons**  ???

3. Otherwise, we have no possibility to have a reference to a parameter from a documentation of class since having a reference to parameters in a documentation of class can have no sense. References to parameters from the documentation of class should be prohibited .
   However, such a reference should be allowed in a doc tag section `@constructor`
```kotlin
/**
* [abc] and [A.abc] should refer to the property
* [p2] is unresolved 
* @constructor [abc] should refer to the parameter
*/
class A(val abc: String, p2: Int) {
// here no access to parameters
}
```
**Pros/Cons**  ???


## Other languages

### Javadoc
[JavaDoc Documentation Comment Specification: References](https://docs.oracle.com/en/java/javase/22/docs/specs/javadoc/doc-comment-spec.html#references) describes the specification of Javadoc references a little:
> the parameter types and parentheses can be omitted if the method or constructor is not overloaded and the name is not also that of a field or enum member in the same class or interface.

Homewer, [Javadoc's style guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html#styleguide) allows to omit parentheses for the general form of methods and constructors. In this case, a ambiguous reference will lead to:
- a field if it exist
- otherwise,  to the first occurrence of overload in the code.
  For example,
```java
/**  
 * {@link #f} references to f(int p)
 */
 public class JavaClassA {  
    public void f(int p) {}
    /**  
		 * {@link #f} references to f(int p)
		 */ 
    public void f() {}  
}
```
Also, by the specification, `#` may be omitted for members:
```java
/**  
 * {@link f} references to the field f
 */
 public class JavaClassB {  
    public void f(int p) {}  
    public void f() {}  
    public int f = 0;
}
```

Meanwhile, a class always have a priority:
```java
/**  
 * {@link JavaClassA2} leads to the class  
 * {@link #JavaClassA2} leads to the field  
 */
 public class JavaClassA2 {  
    public void JavaClassA2(int p) {}  
    public JavaClassA2() {}  
    public int JavaClassA2 = 0;  
}
```
Also, Javadoc does not have references to function parameters.


### JavaScript (JSDoc)

JSDoc does not have such a problem since it has a unique identifier like a fully qualified path in Kotlin.

For `@link` tag ( https://jsdoc.app/tags-inline-link ) there is a namepath. A namepath provides a way to do so and disambiguate between instance members, static members and inner variables. See [https://jsdoc.app/about-namepaths](https://jsdoc.app/about-namepaths)



### Python (Sphinx)

The Python allows to have a cross-reference via the markup see [https://www.sphinx-doc.org/en/master/usage/domains/python.html#cross-referencing-python-objects](https://www.sphinx-doc.org/en/master/usage/domains/python.html#cross-referencing-python-objects)  
There are some roles:  `:py:class` `:py:func`  `:py:meth:` `:py:attr:` and so on.

> Normally, names in these roles are searched first without any further qualification, then with the current module name prepended, then with the current module and class name (if any) prepended.

> If you prefix the name with a dot, this order is reversed. For example, in the documentation of Python’s [codecs](https://docs.python.org/3/library/codecs.html#module-codecs) module, :py:func:`open` always refers to the built-in function, while :py:func:`.open` refers to [codecs.open()](https://docs.python.org/3/library/codecs.html#codecs.open).

> Also, if the name is prefixed with a dot, and no exact match is found, the target is taken as a suffix and all object names with that suffix are searched. For example, :py:meth:`.TarFile.close` references the tarfile.TarFile.close() function, even if the current module is not tarfile. Since this can get ambiguous, if there is more than one possible match, you will get a warning from Sphinx.

### Swift
TODO

### C#

It has XML documentation [https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/documentation-comments](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/documentation-comments)

The `cref` attribute is used to provide a reference to a code element

The documentation generator must respect namespace visibility according to using statements appearing within the source code.

Examples:  
`cref="System.Security.PermissionSet"`
and relative references:
```csharp
public class Point {
/// <summary>
/// This method changes the point's location to
/// the given coordinates. <see cref="Translate"/>
/// </summary>
public void Move(int xPosition, int yPosition) {
...
}

/// <summary>This method changes the point's location by
/// the given x- and y-offsets. <see cref="Move"/>
/// </summary>
public void Translate(int dx, int dy) {
...
}
}
```

As Java, C# does not allow to have a nested class with the same name as enclosing class

Other languages (e.g. Rust, Golang..) do not have the concept of nested classes.


## Appendix



### Visibility
KDoc ignores visibility, i.e. all declarations are public for KDoc references.
Whether  resolving KDoc references should take visibility into account is an open question.

Javadoc can take visibility into account for particular cases (not specified), but for most cases, it works like KDoc.

```java
/**
 * {@link JavaD} is resolved despite `private` and displayed as plain text 
 */
public class JavaB {
    private class JavaC {}
    void f() {}
}
/**
 * {@link JavaC} is unresolved
 * since JavaB.JavaC is private
 * but {@link #f} is resolved and displayed as plain text 
 */
public class JavaA extends JavaB {
}
```
